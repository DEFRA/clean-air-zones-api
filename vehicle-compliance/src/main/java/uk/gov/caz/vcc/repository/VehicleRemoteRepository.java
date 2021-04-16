package uk.gov.caz.vcc.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;

/**
 * Implementation of retrofit2 interface to create a dvla call.
 */
@Service
public class VehicleRemoteRepository {

  private final String apiKey;
  private final VehicleRemoteAsyncRepository vehicleRemoteAsyncRepository;

  public VehicleRemoteRepository(VehicleRemoteAsyncRepository vehicleRemoteAsyncRepository,
      @Value("${dvla-api-key}") String apiKey) {
    this.vehicleRemoteAsyncRepository = vehicleRemoteAsyncRepository;
    this.apiKey = apiKey;
  }

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param vrn used to call
   * @param authenticationToken to allow external dvla calls
   * @return {@link AsyncOp} with prepared REST call.
   */
  public AsyncOp<Vehicle> findByRegistrationNumberAsync(String vrn, String authenticationToken) {
    RemoteVehicleDataRequest requestBody = RemoteVehicleDataRequest.builder()
        .registrationNumber(vrn)
        .build();
    return vehicleRemoteAsyncRepository.findByRegistrationNumberAsync(authenticationToken,
        apiKey, requestBody);
  }
}
