package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
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
public class EntrantPaymentDtoV2 implements EntrantPaymentMethodAndStatusSupplier {

  String vrn;

  UUID cleanAirZoneId;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime cazEntryTimestamp;

  UUID entrantPaymentId;

  PaymentStatus paymentStatus;

  @Nullable
  PaymentMethod paymentMethod;

  @Nullable
  String tariffCode;
}
