package uk.gov.caz.vcc.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.defaultTimeoutInSeconds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.correlationid.MdcCorrelationIdInjector;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.ModVehicleDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesRequestDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesResponseDto;

/**
 * An HTTP based repository that executes REST call to the MOD service in order to fetch required
 * data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HttpBasedModRepository implements ModDataProvider {

  static final int EXIST_BY_VRNS_MAX_BATCH_SIZE = 150;

  private final RetrofitModRepository retrofitModRepository;
  private final AsyncRestService asyncRestService;

  /**
   * A method that checks if given MOD vehicle exists.
   */
  @Override
  public Boolean existsByVrnIgnoreCase(String vrn) {
    return findByVrnIgnoreCase(MdcCorrelationIdInjector.getCurrentValue(), vrn).isPresent();
  }

  /**
   * A method that fetches MOD vehicle from remote service.
   */
  @Override
  public MilitaryVehicle findByVrnIgnoreCase(String vrn) {
    return findByVrnIgnoreCase(MdcCorrelationIdInjector.getCurrentValue(), vrn).orElse(null);
  }

  /**
   * A method that fetches MOD vehicle from remote service with given correlation id.
   */
  private Optional<MilitaryVehicle> findByVrnIgnoreCase(String correlationId, String vrn) {
    checkArgument(isNotBlank(vrn), "VRN for MOD call is blank.");

    AsyncOp<ModVehicleDto> asyncOp = retrofitModRepository
        .findByRegistrationNumberAsync(correlationId, vrn);
    callMod(asyncOp);

    if (asyncOp.getHttpStatus().equals(HttpStatus.OK)) {
      return Optional.of(MilitaryVehicle.fromDto(asyncOp.getResult()));
    }

    if (asyncOp.getHttpStatus().equals(HttpStatus.NOT_FOUND)) {
      log.info("Mod vehicle not found.");
      return Optional.empty();
    }

    log.info("Response code is {}, returning null MilitaryVehicle with correlationId {}",
        asyncOp.getHttpStatus().value(), correlationId);
    throw new ApplicationRuntimeException("Exception while calling MOD");
  }

  /**
   * Checks if given MOD vehicles exist.
   */
  @Override
  public Map<String, Boolean> existByVrns(Set<String> vrns) {
    Iterable<List<String>> batches = Iterables.partition(vrns, EXIST_BY_VRNS_MAX_BATCH_SIZE);
    Builder<String, Boolean> resultBuilder = ImmutableMap.builder();
    for (List<String> batch : batches) {
      Map<String, Boolean> batchResult = processExistByVrnsBatch(batch);
      resultBuilder.putAll(batchResult);
    }
    return resultBuilder.build();
  }

  /**
   * Processes a single batch of VRNs.
   */
  private Map<String, Boolean> processExistByVrnsBatch(List<String> batch) {
    AsyncOp<GetModVehiclesResponseDto> asyncOp = retrofitModRepository
        .findModVehiclesAsync(GetModVehiclesRequestDto.of(batch));
    callMod(asyncOp);
    if (asyncOp.getHttpStatus().equals(HttpStatus.OK)) {
      return toResult(batch, asyncOp.getResult());
    }
    throw new ApplicationRuntimeException("Exception while calling MOD");
  }

  /**
   * Maps the provided {@code checkedVrns} to a map with those vrns as keys and booleans as values,
   * where true means it is a military vehicle, false otherwise.
   */
  private Map<String, Boolean> toResult(List<String> checkedVrns,
      GetModVehiclesResponseDto result) {
    Map<String, ModVehicleDto> militaryVehicles = result.getVehicles();
    return checkedVrns.stream()
        .collect(Collectors.toMap(Function.identity(), militaryVehicles::containsKey));
  }

  /**
   * Starts and awaits for all async requests to MOD.
   */
  private <T> void callMod(AsyncOp<T> asyncOp) {
    long timeout = defaultTimeoutInSeconds();
    try {
      asyncRestService.startAndAwaitAll(singletonList(asyncOp), timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }
  }
}