package uk.gov.caz.vcc.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Entity which contains exemption reason and corresponding Id for reporting.
 */
@Value
@Builder(toBuilder = true)
public class ReportingExemptionReason {
  
  UUID exemptionReasonId;
  
  String exemptionReason;

}
