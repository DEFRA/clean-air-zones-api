package uk.gov.caz.vcc.repository;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.DvlaVehiclesInformation;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData.NtrAndDvlaDataBuilder;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

/**
 * Repository for calls to remote DVLA and NTR API.
 */
@Service
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api", havingValue = "true",
    matchIfMissing = false)
@Slf4j
public class LicenseAndVehicleRemoteRepository implements LicenseAndVehicleRepository {

  private static final int MAX_DVLA_UNAUTHORIZED_CALL_RETRIES = 1;

  private final NationalTaxiRegisterAsyncRepository nationalTaxiRegisterAsyncRepository;
  private final VehicleRemoteRepository vehicleRemoteRepository;
  private final AsyncRestService asyncRestService;
  private final VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator;
  private final int maxDvlaConcurrentCalls;
  private final long timeout;
  
  /**
   * Initializes new instance of {@link LicenseAndVehicleRemoteRepository} class.
   */
  public LicenseAndVehicleRemoteRepository(
      NationalTaxiRegisterAsyncRepository nationalTaxiRegisterAsyncRepository,
      VehicleRemoteRepository vehicleRemoteRepository,
      AsyncRestService asyncRestService,
      VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator,
      @Value("${services.remote-vehicle-data.max-dvla-concurrent-calls:10}")
          Integer maxDvlaConcurrentCalls,
      @Value("${services.connection-timeout-seconds:29L}")
          long timeout) {
    this.nationalTaxiRegisterAsyncRepository = nationalTaxiRegisterAsyncRepository;
    this.vehicleRemoteRepository = vehicleRemoteRepository;
    this.asyncRestService = asyncRestService;
    this.remoteAuthenticationTokenGenerator = remoteAuthenticationTokenGenerator;
    this.maxDvlaConcurrentCalls = maxDvlaConcurrentCalls;
    this.timeout = timeout;
  }

  /**
   * Method to call remote ntr and dvla api for single vrn.
   *
   * @param vrn used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  @Override
  public NtrAndDvlaData findLicenseAndVehicle(String vrn, String authenticationToken) {
    return findLicenseAndVehicle(singletonList(vrn), authenticationToken);
  }

  @Override
  public NtrAndDvlaData findLicenseAndVehicle(List<String> vrns, String authenticationToken) {
    return findLicenseAndVehicleAndMeasureExecutionTime(vrns, authenticationToken);
  }

  /**
   * Method to call remote ntr and dvla api for multiple vrns.
   *
   * @param vrns used to call api
   * @param authenticationToken needed to call dvla api
   * @param attemptedRetries If a 401 response is received the method will recurse and increment
   *     this value.
   * @return {@link NtrAndDvlaData}
   * @see LicenseAndVehicleRepository#findLicenseAndVehicle(List, String)
   */
  private NtrAndDvlaData findLicenseAndVehicle(List<String> vrns, String authenticationToken,
      int attemptedRetries) {
    log.info("Start process of finding Licences and Vehicles for provided [{}] VRNs", vrns.size());

    NtrAndDvlaDataBuilder ntrAndDvlaDataBuilder = NtrAndDvlaData.builder();
    vrns.stream().forEach(vrn -> {
      ntrAndDvlaDataBuilder.ntrLicence(vrn,
          nationalTaxiRegisterAsyncRepository.findByRegistrationNumberAsync(vrn));
      ntrAndDvlaDataBuilder.dvlaVehicle(vrn,
          vehicleRemoteRepository.findByRegistrationNumberAsync(vrn, authenticationToken));
    });
    NtrAndDvlaData ntrAndDvlaData = ntrAndDvlaDataBuilder.build();

    try {
      asyncRestService.startAndAwaitAll(mergeNtrAndDvlaAsyncOps(ntrAndDvlaData), timeout,
          TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }

    List<AsyncOp<Vehicle>> anyUnauthorized = ntrAndDvlaData.dvlaAsyncOps().stream()
        .filter(dvlaOp -> dvlaOp.getHttpStatus() == HttpStatus.UNAUTHORIZED)
        .collect(Collectors.toList());

    if (anyUnauthorized.isEmpty()) {
      return ntrAndDvlaData;
    }

    if (attemptedRetries == 0) {
      anyUnauthorized.forEach(dvlaOp -> log.warn(
          "DVLA API returned UNAUTHORIZED status code during async processing of "
              + "request with ID: {}. Retrying...", dvlaOp.getIdentifier()));
      String newToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();
      return findLicenseAndVehicle(vrns, newToken, 1);
    }
    throw new ExternalServiceCallException("Authentication error on Async call to DVLA - 401 "
        + "response received.");
  }

  private NtrAndDvlaData findLicenseAndVehicleAndMeasureExecutionTime(List<String> vrns,
      String authenticationToken) {
    return measureExecutionTime("Find licences and DVL vehicles", () ->
        findLicenseAndVehicle(vrns, authenticationToken, 0));
  }

  @Override
  public NtrAndDvlaData findVehicles(List<String> vrns, String authenticationToken) {
    NtrAndDvlaDataBuilder ntrAndDvlaDataBuilder = NtrAndDvlaData.builder();
    for (String vrn : vrns) {
      ntrAndDvlaDataBuilder.dvlaVehicle(vrn,
          vehicleRemoteRepository.findByRegistrationNumberAsync(vrn, authenticationToken));
    }
    vrns.stream().forEach(vrn -> ntrAndDvlaDataBuilder.dvlaVehicle(vrn,
        vehicleRemoteRepository.findByRegistrationNumberAsync(vrn, authenticationToken)));
    NtrAndDvlaData ntrAndDvlaData = ntrAndDvlaDataBuilder.build();
    executeAsyncOps(new ArrayList<>(ntrAndDvlaData.getDvlaVehicles().values()), timeout);
    return ntrAndDvlaData;
  }

  @Override
  public SingleDvlaVehicleData findDvlaVehicle(String vrn, String authenticationToken) {
    return measureExecutionTime("Find single DVL vehicle", () ->
        findDvlaVehicle(vrn, authenticationToken, 0));
  }

  private SingleDvlaVehicleData findDvlaVehicle(String vrn, String authToken,
      int attemptedRetries) {
    AsyncOp<Vehicle> asyncOp = vehicleRemoteRepository.findByRegistrationNumberAsync(vrn,
        authToken);

    executeAsyncOps(singletonList(asyncOp), timeout);
    
    if (asyncOp.getHttpStatus() == HttpStatus.UNAUTHORIZED) {
      if (attemptedRetries == 1) {
        throw new ExternalServiceCallException("Authentication error on Async call to DVLA - 401 "
            + "response received.");
      }
      // Failed during the first attempt, try generating a new token and repeat
      log.warn("DVLA API call returned UNAUTHORIZED status, retrying with a new token");
      return findDvlaVehicle(vrn, remoteAuthenticationTokenGenerator.getAuthenticationToken(), 1);
    }

    if (asyncOp.hasError()) {
      return SingleDvlaVehicleData.failure(asyncOp.getHttpStatus(), asyncOp.getError());
    }
    return SingleDvlaVehicleData.success(asyncOp.getResult());
  }

  @Override
  public DvlaVehiclesInformation findDvlaVehiclesInBulk(Collection<String> vrns) {
    return measureExecutionTime("Find bulk DVLA vehicles", () ->
        findDvlaVehiclesInBulkInternal(vrns));
  }

  /**
   * Partitions list of VRNs into small batches and executes concurrent calls for each VRN in a
   * batch to get vehicle details from DVLA.
   */
  private DvlaVehiclesInformation findDvlaVehiclesInBulkInternal(Collection<String> vrns) {
    String authToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();
    val bulkDvlaCallBatches = Iterables.partition(vrns, maxDvlaConcurrentCalls);
    val resultBuilder = ImmutableMap.<String, SingleDvlaVehicleData>builder();
    for (val bulkDvlaCallVrns : bulkDvlaCallBatches) {
      for (int attempt = 0; attempt <= MAX_DVLA_UNAUTHORIZED_CALL_RETRIES; attempt++) {
        val asyncOps = buildConcurrentCallsToDvla(authToken, bulkDvlaCallVrns);
        val failure = startAndWaitAndReturnFailureIfFailed(asyncOps);
        if (failure.isPresent()) {
          return failure.get();
        }
        if (anyHasUnauthorizedError(asyncOps)) {
          authToken = throwIfReachedRetriesLimitOrGenerateNewAuthToken(attempt);
        } else {
          appendAsyncOpResultsToFinalResultsBuilder(resultBuilder, asyncOps);
          break;
        }
      }
    }
    return DvlaVehiclesInformation.success(resultBuilder.build());
  }

  /**
   * Traverses each {@link AsyncOp} and maps it to {@link SingleDvlaVehicleData} either as success
   * or as a failure and appends to result builder.
   */
  private void appendAsyncOpResultsToFinalResultsBuilder(
      Builder<String, SingleDvlaVehicleData> resultBuilder,
      ImmutableMap<String, AsyncOp<Vehicle>> asyncOps) {
    asyncOps.forEach((vrn, vehicleAsyncOp) -> {
      if (vehicleAsyncOp.hasError()) {
        resultBuilder.put(vrn, SingleDvlaVehicleData
            .failure(vehicleAsyncOp.getHttpStatus(), vehicleAsyncOp.getError()));
      } else {
        resultBuilder.put(vrn, SingleDvlaVehicleData.success(vehicleAsyncOp.getResult()));
      }
    });
  }

  /**
   * If {@code} attempt reached limit of retries throws {@link ExternalServiceCallException}.
   * Otherwise generates new authentication token and allows to retry again.
   */
  private String throwIfReachedRetriesLimitOrGenerateNewAuthToken(int attempt) {
    String authToken;
    if (attempt == MAX_DVLA_UNAUTHORIZED_CALL_RETRIES) {
      throw new ExternalServiceCallException(
          "Authentication error on Async call to DVLA - 401 "
              + "response received.");
    }
    // Failed during the first attempt, try generating a new token and repeat
    log.warn("BULK DVLA API call returned UNAUTHORIZED status, retrying with a new token");
    authToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();
    return authToken;
  }

  /**
   * Returns true if any {@link AsyncOp} has an error and the error is 401 UNAUTHORIZED.
   */
  private boolean anyHasUnauthorizedError(ImmutableMap<String, AsyncOp<Vehicle>> asyncOps) {
    return asyncOps.values().stream().anyMatch(
        asyncOp -> asyncOp.hasError() && asyncOp.getHttpStatus() == HttpStatus.UNAUTHORIZED);
  }

  /**
   * Starts and awaits all {@link AsyncOp}. If global operation failed returns {@link
   * DvlaVehiclesInformation} as failure. Otherwise returns Optional.empty().
   */
  private Optional<DvlaVehiclesInformation> startAndWaitAndReturnFailureIfFailed(
      ImmutableMap<String, AsyncOp<Vehicle>> asyncOps) {
    try {
      executeAsyncOps(newArrayList(asyncOps.values()), timeout);
    } catch (ExternalServiceCallException externalServiceCallException) {
      log.error("Error in bulk DVLA call", externalServiceCallException);
      return Optional.of(DvlaVehiclesInformation.failure());
    }
    return Optional.empty();
  }

  /**
   * Builds map of VRNs to {@link AsyncOp}.
   */
  private ImmutableMap<String, AsyncOp<Vehicle>> buildConcurrentCallsToDvla(String authToken,
      List<String> bulkDvlaCallVrns) {
    Builder<String, AsyncOp<Vehicle>> asyncOpsCallBuilder = ImmutableMap.builder();
    bulkDvlaCallVrns.stream().forEach(vrn -> asyncOpsCallBuilder
        .put(vrn, vehicleRemoteRepository.findByRegistrationNumberAsync(vrn, authToken)));
    val asyncOps = asyncOpsCallBuilder.build();
    return asyncOps;
  }

  /**
   * Executes all {@link AsyncOp} and awaits until {@code timeout} expires.
   */
  private <T> void executeAsyncOps(List<AsyncOp<T>> asyncOps, long timeout) {
    try {
      asyncRestService.startAndAwaitAll(asyncOps, timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }
  }

  /**
   * Extracts {@link AsyncOp} calls for NTR and DVLA and merges them into one list.
   */
  private List<AsyncOp> mergeNtrAndDvlaAsyncOps(NtrAndDvlaData ntrAndDvlaData) {
    return Stream
        .concat(ntrAndDvlaData.ntrAsyncOps().stream(), ntrAndDvlaData.dvlaAsyncOps().stream())
        .collect(Collectors.toList());
  }

  private <T> T measureExecutionTime(String actionLabel, Supplier<T> action) {
    Stopwatch timer = Stopwatch.createStarted();
    T result = action.get();
    log.info("{} took {}ms", actionLabel, timer.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }
}
