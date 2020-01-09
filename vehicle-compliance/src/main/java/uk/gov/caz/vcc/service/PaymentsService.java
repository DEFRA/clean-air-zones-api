package uk.gov.caz.vcc.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.PaymentStatusResponseDto;
import uk.gov.caz.vcc.repository.PaymentsRepository;

/**
 * Service that is responsible for integration with payments service.
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaymentsService {

  private final PaymentsRepository paymentsRepository;

  /**
   * Get payment status from payments service.
   *
   * @param request {@link PaymentStatusRequestDto}.
   * @return {@link PaymentStatus}.
   */
  public PaymentStatus registerVehicleEntryAndGetPaymentStatus(PaymentStatusRequestDto request) {
    PaymentStatus paymentStatus = paymentsRepository
        .registerVehicleEntryAndGetPaymentStatus(request)
        .map(PaymentStatusResponseDto::getStatus)
        .orElse(PaymentStatus.NOT_PAID);
    log.info("Payments status '{}' for '{}' VRN", paymentStatus, request.getVrn());
    return paymentStatus;
  }
}