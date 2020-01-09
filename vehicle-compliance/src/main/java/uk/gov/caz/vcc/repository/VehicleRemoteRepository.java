package uk.gov.caz.vcc.repository;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;

/**
 * Implementation of retrofit2 interface to create a dvla call.
 */
@Service
public class VehicleRemoteRepository {

  @Value("${dvla-api-endpoint}")
  private String dvlaApiEndpoint;

  @Value("${dvla-api-key}")
  private String dvlaApiKey;

  private final VehicleRepository vehicleRepository;

  public VehicleRemoteRepository(VehicleRepository vehicleRepository) {
    this.vehicleRepository = vehicleRepository;
  }

  /**
   * Method to create rerofit2 call {@link Call}.
   *
   * @param vrn used to call
   * @param authenticationToken to allow extrenal dvla calls
   * @return {@link Call}
   */
  public Call<Vehicle> findByRegistrationNumber(String vrn, String authenticationToken) {
    RemoteVehicleDataRequest requestBody = RemoteVehicleDataRequest.builder()
        .registrationNumber(vrn).build();
    return vehicleRepository
        .findByRegistrationNumber(UUID.randomUUID().toString(), authenticationToken, dvlaApiKey,
            requestBody);
  }
}
