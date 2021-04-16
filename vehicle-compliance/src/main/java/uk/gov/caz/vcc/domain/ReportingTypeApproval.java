package uk.gov.caz.vcc.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Entity which contains type approval and corresponding Id for reporting.
 */
@Value
@Builder(toBuilder = true)
public class ReportingTypeApproval {
  
  UUID typeApprovalId;
  
  String typeApproval;

}
