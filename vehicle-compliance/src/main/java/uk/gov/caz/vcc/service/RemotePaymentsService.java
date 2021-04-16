package uk.gov.caz.vcc.service;

import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.repository.PaymentsRepository;

/**
 * Service that is responsible for integration with payments service.
 */
@Slf4j
@AllArgsConstructor
public class RemotePaymentsService implements PaymentsService {

  private static final EntrantPaymentDtoV1 DEFAULT_V1_RESPONSE = EntrantPaymentDtoV1.builder()
      .paymentStatus(PaymentStatus.NOT_PAID)
      .paymentMethod(PaymentMethod.NULL)
      .build();

  private final PaymentsRepository paymentsRepository;
  private final AsyncRestService asyncRestService;

  /**
   * Get payment status from payments service.
   *
   * @param request {@link EntrantPaymentRequestDto}.
   * @return {@link PaymentStatus}.
   */
  @Override
  public final EntrantPaymentDtoV1 registerVehicleEntryAndGetPaymentStatus(
      EntrantPaymentRequestDto request) {
    return Iterables.getFirst(callAndWaitV1(Collections.singletonList(request)),
        DEFAULT_V1_RESPONSE);
  }

  @Override
  public List<EntrantPaymentDtoV2> registerVehicleEntriesAndGetPaymentStatus(
      List<EntrantPaymentRequestDto> requests) {
    if (requests.isEmpty()) {
      return Collections.emptyList();
    }
    return callAndWaitV2(requests);
  }

  protected final List<EntrantPaymentDtoV2> callAndWaitV2(List<EntrantPaymentRequestDto> requests) {
    AsyncOp<List<EntrantPaymentDtoV2>> paymentStatusResponse = paymentsRepository
        .registerVehicleEntryAsyncV2(requests);
    return callAndProcessResponse(paymentStatusResponse);
  }

  protected final List<EntrantPaymentDtoV1> callAndWaitV1(List<EntrantPaymentRequestDto> requests) {
    AsyncOp<List<EntrantPaymentDtoV1>> paymentStatusResponse = paymentsRepository
        .registerVehicleEntryAsyncV1(requests);
    return callAndProcessResponse(paymentStatusResponse);
  }

  private <T> List<T> callAndProcessResponse(AsyncOp<List<T>> paymentStatusResponse) {
    callPayment(paymentStatusResponse);

    if (paymentStatusResponse.hasError()) {
      log.error("Payment call return error {}, code: {}", paymentStatusResponse.getError(),
          paymentStatusResponse.getHttpStatus());
      throw new ExternalServiceCallException();
    }
    return paymentStatusResponse.getResult();
  }

  /**
   * Starts and awaits for all async requests to PSR.
   */
  private <T> void callPayment(AsyncOp<List<T>> asyncOp) {
    long timeout = calculateTimeoutInSeconds();
    try {
      asyncRestService.startAndAwaitAll(Collections.singletonList(asyncOp), timeout,
          TimeUnit.SECONDS);
    } catch (Exception exception) {
      log.error("Unexpected exception occurs ", exception);
      throw new ExternalServiceCallException(exception);
    }
  }

  /**
   * Helper method to get timeout seconds.
   *
   * @return timeout
   */
  static long calculateTimeoutInSeconds() {
    return 5L;
  }
}