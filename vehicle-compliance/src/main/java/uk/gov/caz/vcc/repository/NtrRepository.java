package uk.gov.caz.vcc.repository;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Retrofit2 repository to create a ntr call.
 */
public interface NtrRepository {

  /**
   * Method to creat retrofit2 ntr call.
   *
   * @param vrn to call
   * @return {@link Call}
   */
  @Headers("Content-Type: application/json")
  @GET("v1/vehicles/{vrn}/licence-info")
  Call<TaxiPhvLicenseInformationResponse> findByRegistrationNumber(
      @Header("X-Correlation-ID") String correlationId,
      @Path("vrn") String vrn);
}
