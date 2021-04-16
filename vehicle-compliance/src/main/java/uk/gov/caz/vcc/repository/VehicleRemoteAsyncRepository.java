package uk.gov.caz.vcc.repository;

import org.springframework.http.HttpHeaders;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;

/**
 * Retrofit2 repository to create a dvla call.
 */
public interface VehicleRemoteAsyncRepository {

  /**
   * Method to create retrofit2 DVLA call.
   *
   * @param token to auth dvla call
   * @param dvlaApiKey api key to allow external calls
   * @param request {@link RemoteVehicleDataRequest}
   * @return retrofit2 call {@link Call} of internal vehicle {@link Vehicle}
   */
  @Headers("Content-Type: application/json")
  @POST(".")
  Call<Vehicle> findByRegistrationNumber(@Header(HttpHeaders.AUTHORIZATION) String token,
      @Header("x-api-key") String dvlaApiKey,
      @Body RemoteVehicleDataRequest request);


  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param token to auth dvla call
   * @param dvlaApiKey api key to allow external calls
   * @param request {@link RemoteVehicleDataRequest}
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<Vehicle> findByRegistrationNumberAsync(String token,
      String dvlaApiKey, RemoteVehicleDataRequest request) {
    return AsyncOp.from("DVLA: " + request.getRegistrationNumber().hashCode(),
        findByRegistrationNumber(token, dvlaApiKey, request));
  }
}
