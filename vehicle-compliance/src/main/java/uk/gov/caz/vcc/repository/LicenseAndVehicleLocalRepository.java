package uk.gov.caz.vcc.repository;

import static uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.defaultTimeoutInSeconds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
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


/**
 * Repository calls local dvla and remote ntr api.
 */
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api", havingValue = "false",
    matchIfMissing = true)
@AllArgsConstructor
@Service
public class LicenseAndVehicleLocalRepository implements LicenseAndVehicleRepository {

  private final NationalTaxiRegisterAsyncRepository nationalTaxiRegisterAsyncRepository;
  private final LocalVehicleDetailsRepository localVehicleDetailsRepository;
  private final AsyncRestService asyncRestService;

  /**
   * Method to call remote NTR and DVLA API for single vrn.
   *
   * @param vrn used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  public NtrAndDvlaData findLicenseAndVehicle(String vrn, String authenticationToken) {
    return findLicenseAndVehicle(Collections.singletonList(vrn), authenticationToken);
  }

  /**
   * Method to call remote ntr and local dvla api for multiple vrns.
   *
   * @param vrns used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  public NtrAndDvlaData findLicenseAndVehicle(List<String> vrns, String authenticationToken) {

    NtrAndDvlaDataBuilder ntrAndDvlaDataBuilder = NtrAndDvlaData.builder();
    vrns.stream().forEach(vrn -> {
      ntrAndDvlaDataBuilder.ntrLicence(vrn,
          nationalTaxiRegisterAsyncRepository.findByRegistrationNumberAsync(vrn));
      ntrAndDvlaDataBuilder.dvlaVehicle(vrn, wrapInDvlaFromLocalDb(vrn));
    });

    NtrAndDvlaData ntrAndDvlaData = ntrAndDvlaDataBuilder.build();
    callNtr(ntrAndDvlaData);
    return ntrAndDvlaData;
  }

  /**
   * Creates {@link AsyncOp} for DVLA REST calls.
   */
  private AsyncOp<Vehicle> wrapInDvlaFromLocalDb(String vrn) {
    String id = UUID.randomUUID().toString();
    return localVehicleDetailsRepository.findByRegistrationNumber(vrn)
        .map(vehicle -> AsyncOp.asCompletedAndSuccessful("DVLA: " + id, HttpStatus.OK, vehicle))
        .orElseGet(() -> AsyncOp.asCompletedAndFailed("DVLA: " + id, HttpStatus.NOT_FOUND,
            "Vehicle not found"));
  }

  @Override
  public NtrAndDvlaData findVehicles(List<String> vrns, String authenticationToken) {
    NtrAndDvlaDataBuilder ntrAndDvlaDataBuilder = NtrAndDvlaData.builder();
    for (String vrn : vrns) {
      ntrAndDvlaDataBuilder.dvlaVehicle(vrn, wrapInDvlaFromLocalDb(vrn));
    }
    return ntrAndDvlaDataBuilder.build();
  }

  @Override
  public SingleDvlaVehicleData findDvlaVehicle(String vrn, String authenticationToken) {
    return localVehicleDetailsRepository.findByRegistrationNumber(vrn)
        .map(SingleDvlaVehicleData::success)
        .orElseGet(() -> SingleDvlaVehicleData.failure(HttpStatus.NOT_FOUND, "Not exists"));
  }

  @Override
  public DvlaVehiclesInformation findDvlaVehiclesInBulk(Collection<String> vrns) {
    if (vrns.isEmpty()) {
      return DvlaVehiclesInformation.success(Collections.emptyMap());
    }

    Builder<String, SingleDvlaVehicleData> resultBuilder = ImmutableMap.builder();
    vrns.forEach(vrn -> resultBuilder.put(vrn, getVehicleOr404IfNotFound(vrn)));
    return DvlaVehiclesInformation.success(resultBuilder.build());
  }

  /**
   * Returns {@link SingleDvlaVehicleData} with {@link Vehicle} details or {@link
   * SingleDvlaVehicleData} with error of type 404 if vehicle does not exist.
   */
  private SingleDvlaVehicleData getVehicleOr404IfNotFound(String vrn) {
    return localVehicleDetailsRepository.findByRegistrationNumber(vrn)
        .map(vehicle -> SingleDvlaVehicleData.success(vehicle))
        .orElse(SingleDvlaVehicleData.failure(HttpStatus.NOT_FOUND, "Not exists"));
  }

  /**
   * Starts and awaits for all async requests to NTR.
   */
  private void callNtr(NtrAndDvlaData responses) {
    long timeout = defaultTimeoutInSeconds();
    try {
      asyncRestService.startAndAwaitAll(responses.ntrAsyncOps(), timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }
  }
}