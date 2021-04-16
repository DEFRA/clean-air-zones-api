package uk.gov.caz.vcc.repository;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.vcc.dto.ModVehicleDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesRequestDto;
import uk.gov.caz.vcc.dto.mod.GetModVehiclesResponseDto;

/**
 * A class that uses Retrofit specific classes in order to provide HTTP logic for fetching MOD data.
 */
public interface RetrofitModRepository {

  /**
   * Method to create retrofit2 MOD call.
   *
   * @param vrn to call
   * @return {@link Call}
   */
  @Headers("Content-Type: application/json")
  @GET("v1/mod/{vrn}")
  Call<ModVehicleDto> findModVehicle(
      @Header("X-Correlation-ID") String correlationId,
      @Path("vrn") String vrn);

  /**
   * Finds details of MOD vehicles by their registration numbers.
   */
  @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
  })
  @POST("v1/mod/search")
  Call<GetModVehiclesResponseDto> findModVehicles(@Body GetModVehiclesRequestDto request);

  /**
   * Finds details of MOD vehicles by their registration numbers in an async manner.
   */
  default AsyncOp<GetModVehiclesResponseDto> findModVehiclesAsync(GetModVehiclesRequestDto req) {
    return AsyncOp.from("MOD: " + req.hashCode(), findModVehicles(req));
  }

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param correlationId UUID identifying incoming request.
   * @param vrn Vehicle Registration Number.
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<ModVehicleDto> findByRegistrationNumberAsync(
      String correlationId, String vrn) {
    return AsyncOp.from("MOD: " + correlationId, findModVehicle(correlationId, vrn));
  }
}
