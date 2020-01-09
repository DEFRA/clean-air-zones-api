package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Wrapper class for payment status response object.
 */
@Value
@Builder
public class PaymentStatusResponseDto {

  /**
   * Payment status obtained from payments service.
   */
  PaymentStatus status;
}
