
package uk.gov.caz.vcc.repository;

import java.util.UUID;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.ntr.GetLicencesInfoRequestDto;
import uk.gov.caz.vcc.dto.ntr.GetLicencesInfoResponseDto;

/**
 * Retrofit2 repository to create a ntr call.
 */
public interface NationalTaxiRegisterAsyncRepository {

  /**
   * Method to create retrofit2 ntr call.
   *
   * @param vrn to call
   * @return {@link Call}
   */
  @Headers("Content-Type: application/json")
  @GET("v1/vehicles/{vrn}/licence-info")
  Call<TaxiPhvLicenseInformationResponse> findByRegistrationNumber(@Path("vrn") String vrn);

  @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
  })
  @POST("v1/vehicles/licences-info/search")
  Call<GetLicencesInfoResponseDto> findByRegistrationNumbers(
      @Body GetLicencesInfoRequestDto request);

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param vrn Vehicle Registration Number.
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<TaxiPhvLicenseInformationResponse> findByRegistrationNumberAsync(String vrn) {
    return AsyncOp.from("NTR: " + UUID.randomUUID().toString(), findByRegistrationNumber(vrn));
  }

  default AsyncOp<GetLicencesInfoResponseDto> findByRegistrationNumbersAsync(
      GetLicencesInfoRequestDto request) {
    return AsyncOp.from("Request: " + request.hashCode(), findByRegistrationNumbers(request));
  }
}
