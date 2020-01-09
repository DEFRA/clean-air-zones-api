package uk.gov.caz.vcc.repository;

import static uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.calculateTimeout;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncRequest;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.async.rest.AsyncResponses;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;


/**
 * Repository calls local dvla and remote ntr api.
 */
@Slf4j
@ConditionalOnProperty(
    value = "services.remote-vehicle-data.use-remote-api", havingValue = "false",
    matchIfMissing = true)
@AllArgsConstructor
@Service
public class LicenseAndLocalVehicleRepository implements LicenseAndVehicleRepository {

  private NtrRemoteRepository ntrRemoteRepository;
  private LocalVehicleDetailsRepository localVehicleDetailsRepository;
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
  public LicenseAndVehicleResponse findLicenseAndVehicle(String vrn, String authenticationToken,
      String correlationId) {
    return findLicenseAndVehicle(Collections.singletonList(vrn), authenticationToken, correlationId);
  }

  /**
   * Method to call remote ntr and local dvla api for multiple vrns.
   *
   * @param vrns used to call api
   * @param authenticationToken needed to call dvla api
   * @param correlationId to correlate requests
   * @return {@link LicenseAndVehicleResponse}
   * @see LicenseAndVehicleRepository#findLicenseAndVehicle(List, String, String)
   */
  public LicenseAndVehicleResponse findLicenseAndVehicle(List<String> vrns,
      String authenticationToken,
      String correlationId) {

    List<AsyncRequest<TaxiPhvLicenseInformationResponse>> ntrAsyncRequests = vrns.stream()
        .map(vrn -> new AsyncRequest<>(vrn,
            ntrRemoteRepository.getLicenseInfo(correlationId, vrn)))
        .collect(Collectors.toList());

    AsyncResponses<TaxiPhvLicenseInformationResponse> ntrResponses = asyncRestService
        .call(ntrAsyncRequests);

    Map<String, AsyncResponse<Vehicle>> vehicles = new HashMap<>();
    for (String vrn : vrns) {
      AsyncResponse<Vehicle> asyncResponse = localVehicleDetailsRepository
          .findByRegistrationNumber(vrn)
          .map(AsyncResponse::success)
          .orElseGet(() -> AsyncResponse.failure("Vehicle not found", HttpStatus.NOT_FOUND));
      vehicles.put(vrn, asyncResponse);
    }
    long timeout = calculateTimeout(vrns);
    return new LicenseAndVehicleResponse(getResponse(ntrResponses, timeout),
        vehicles);
  }
}
