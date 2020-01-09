package uk.gov.caz.vcc.repository;

import static java.util.Collections.singletonList;
import static uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.calculateTimeout;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncRequest;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.async.rest.AsyncResponses;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Repository calls remote dval and ntr api.
 */
@AllArgsConstructor
@Service
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api", havingValue = "true", matchIfMissing = false)
@Slf4j
public class LicenseAndVehicleRemoteRepository implements LicenseAndVehicleRepository {

  private NtrRemoteRepository ntrRemoteRepository;
  private VehicleRemoteRepository vehicleRemoteRepository;
  private AsyncRestService asyncRestService;

  /**
   * Method to call remote ntr and dvla api for single vrn.
   *
   * @param vrn used to call api
   * @param authenticationToken needed to call dvla api
   * @param correlationId to correlate requests
   * @return {@link LicenseAndVehicleResponse}
   * @see LicenseAndVehicleRepository#findLicenseAndVehicle(String, String, String)
   */
  @Override
  public LicenseAndVehicleResponse findLicenseAndVehicle(String vrn, String authenticationToken,
      String correlationId) {
    return findLicenseAndVehicle(singletonList(vrn), authenticationToken, correlationId);
  }

  /**
   * Method to call remote ntr and dvla api for multiple vrns.
   *
   * @param vrns used to call api
   * @param authenticationToken needed to call dvla api
   * @param correlationId to correlate requests
   * @return {@link LicenseAndVehicleResponse}
   * @see LicenseAndVehicleRepository#findLicenseAndVehicle(List, String, String)
   */
  @Override
  public LicenseAndVehicleResponse findLicenseAndVehicle(List<String> vrns,
      String authenticationToken,
      String correlationId) {
    log.info("Start processing vrns {}", vrns);
    List<AsyncRequest<TaxiPhvLicenseInformationResponse>> ntrAsyncRequests = vrns.stream()
        .map(vrn -> new AsyncRequest<>(vrn, ntrRemoteRepository.getLicenseInfo(correlationId, vrn)))
        .collect(Collectors.toList());

    List<AsyncRequest<Vehicle>> vehicleAsyncRequests = vrns.stream().map(
        vrn -> new AsyncRequest<>(vrn,
            vehicleRemoteRepository.findByRegistrationNumber(vrn, authenticationToken)))
        .collect(Collectors.toList());

    AsyncResponses<TaxiPhvLicenseInformationResponse> ntrResponses = asyncRestService
        .call(ntrAsyncRequests);

    AsyncResponses<Vehicle> vehicleResponses = asyncRestService.call(vehicleAsyncRequests);

    long timeout = calculateTimeout(vrns);

    Map<String, AsyncResponse<TaxiPhvLicenseInformationResponse>> licenceInfo = getResponse(
        ntrResponses, timeout);

    Map<String, AsyncResponse<Vehicle>> vehicles = getResponse(vehicleResponses, timeout);

    return new LicenseAndVehicleResponse(licenceInfo, vehicles);
  }
}
