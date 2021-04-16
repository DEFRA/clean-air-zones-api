package uk.gov.caz.vcc.dto;

import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;

/**
 * Entrant payment response object, which contain payment status, payment id and optional payment
 * method.
 */
@Value
@Builder(toBuilder = true)
public class EntrantPaymentDtoV1 implements EntrantPaymentMethodAndStatusSupplier {

  UUID entrantPaymentId;

  PaymentStatus paymentStatus;

  @Nullable
  PaymentMethod paymentMethod;

  @Nullable
  String tariffCode;
}
