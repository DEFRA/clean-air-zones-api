package uk.gov.caz.vcc.repository;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;

/**
 * Retrofit2 repository to create a payment call.
 */
public interface PaymentsRepository {

  /**
   * Method to create retrofit2 payment call.
   *
   * @param paymentStatusRequests list of {@link EntrantPaymentRequestDto}
   * @return list of {@link EntrantPaymentDtoV1}
   */
  @Headers({"Content-Type: application/json"})
  @POST("v1/payments/vehicle-entrants")
  Call<List<EntrantPaymentDtoV1>> registerVehicleEntryV1(
      @Body List<EntrantPaymentRequestDto> paymentStatusRequests);

  /**
   * Method to create retrofit2 payment call.
   *
   * @param paymentStatusRequests list of {@link EntrantPaymentRequestDto}
   * @return list of {@link EntrantPaymentDtoV2}
   */
  @Headers({"Content-Type: application/json"})
  @POST("v1/payments/vehicle-entrants")
  Call<List<EntrantPaymentDtoV2>> registerVehicleEntryV2(
      @Body List<EntrantPaymentRequestDto> paymentStatusRequests);


  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param paymentStatusRequests List of {@link EntrantPaymentRequestDto}.
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<List<EntrantPaymentDtoV1>> registerVehicleEntryAsyncV1(
      List<EntrantPaymentRequestDto> paymentStatusRequests) {
    return AsyncOp.from("PSR: " + paymentStatusRequests.hashCode(),
        registerVehicleEntryV1(paymentStatusRequests));
  }

  /**
   * Wraps REST API call in {@link AsyncOp} making it asynchronous.
   *
   * @param paymentStatusRequests List of {@link EntrantPaymentRequestDto}.
   * @return {@link AsyncOp} with prepared REST call.
   */
  default AsyncOp<List<EntrantPaymentDtoV2>> registerVehicleEntryAsyncV2(
      List<EntrantPaymentRequestDto> paymentStatusRequests) {
    return AsyncOp.from("PSR: " + paymentStatusRequests.hashCode(),
        registerVehicleEntryV2(paymentStatusRequests));
  }

}