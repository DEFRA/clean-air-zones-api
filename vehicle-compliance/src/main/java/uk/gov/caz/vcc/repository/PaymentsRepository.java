package uk.gov.caz.vcc.repository;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.PaymentStatusResponseDto;

/**
 * The class that is responsible for integration with payments service.
 */
@Slf4j
@Repository
public class PaymentsRepository {

  private final RestTemplate paymentsRestTemplate;

  @VisibleForTesting
  public static final String PAYMENT_VEHICLE_ENTRANTS_URL = "/v1/payments/vehicle-entrants";

  private final URI getPaymentStatusUrl;

  /**
   * Public constructor. Builds rest templates for payments repository.
   *
   * @param restTemplateBuilder builder for the separate rest templates
   */
  public PaymentsRepository(RestTemplateBuilder restTemplateBuilder,
      @Value("${services.payments.root-url}") String paymentsRootUrl) {
    this.paymentsRestTemplate = restTemplateBuilder
        .rootUri(paymentsRootUrl + PAYMENT_VEHICLE_ENTRANTS_URL).build();
    this.getPaymentStatusUrl = URI.create(paymentsRootUrl + PAYMENT_VEHICLE_ENTRANTS_URL);
  }

  /**
   * Get payment status from payments service.
   *
   * @param request {@link PaymentStatusRequestDto}.
   * @return {@link PaymentStatusResponseDto} (optional).
   */
  public Optional<PaymentStatusResponseDto> registerVehicleEntryAndGetPaymentStatus(
      PaymentStatusRequestDto request) {
    try {
      log.info("Get payment status for '{}' VRN, start", request.getVrn());
      ResponseEntity<PaymentStatusResponseDto> responseEntity = paymentsRestTemplate
          .exchange(buildRequestEntityForPaymentStatus(request), PaymentStatusResponseDto.class);
      return Optional.ofNullable(responseEntity.getBody());
    } catch (HttpServerErrorException e) {
      log.error("Error {} while getting the payment status for VRN '{}'", e, request.getVrn());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Cannot call Payments Service fir vrn {}", request.getVrn());
      throw new ExternalServiceCallException(e);
    } finally {
      log.info("Get payment status for '{}' VRN, finish", request.getVrn());
    }
  }

  /**
   * Creates a request entity for payment status operation.
   */
  private RequestEntity<PaymentStatusRequestDto> buildRequestEntityForPaymentStatus(
      PaymentStatusRequestDto paymentStatusRequest) {
    return RequestEntity.post(getPaymentStatusUrl)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(paymentStatusRequest);
  }
}