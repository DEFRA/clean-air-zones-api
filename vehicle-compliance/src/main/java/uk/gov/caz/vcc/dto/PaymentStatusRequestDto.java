package uk.gov.caz.vcc.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Wrapper class for single payment status request object.
 */
@Value
@Builder
public class PaymentStatusRequestDto {

  /**
   * The identifiers of the clean air zones.
   */
  UUID cleanZoneId;

  /**
   * ISO-8601 formatted datetime indicating  when the vehicle was witnessed entering the CAZ.
   */
  LocalDateTime cazEntryTimestamp;

  /**
   * String containing unique Vehicle registration number.
   */
  String vrn;
}
