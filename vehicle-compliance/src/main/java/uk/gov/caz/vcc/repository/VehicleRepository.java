package uk.gov.caz.vcc.repository;

import org.springframework.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;

/**
 * Retrofit2 repository to create a dvla call.
 */
public interface VehicleRepository {

  /**
   * Method to creat retrofit2 dvla call.
   *
   * @param correlationId for correlation
   * @param token         to auth dvla call
   * @param dvlaApiKey    api key to allow external calls
   * @param request       {@link RemoteVehicleDataRequest}
   * @return retrofit2 call {@link Call} of internal vehicle {@link Vehicle}
   */
  @Headers("Content-Type: application/json")
  @POST(".")
  Call<Vehicle> findByRegistrationNumber(
      @Header("X-Correlation-ID") String correlationId,
      @Header(HttpHeaders.AUTHORIZATION) String token,
      @Header("x-api-key") String dvlaApiKey,
      @Body RemoteVehicleDataRequest request);
}
