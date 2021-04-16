package uk.gov.caz.vcc.domain.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Domain service for working with vehicle type approvals.
 *
 */
@Service
public class TypeApprovalService {

  private final List<String> exemptTypeApprovals;

  public TypeApprovalService(
      @Value("${application.exempt-type-approvals:T1,T2,T3,T4,T5}") String[] exemptTypeApprovals) {
    this.exemptTypeApprovals = Arrays.asList(exemptTypeApprovals);
  }

  /**
   * Helper method to check if a vehicle's type approval is deemed exempt from charging.
   * 
   * @param typeApproval the type approval string literal of the vehicle.
   * @return boolean indicator for whether the type approval is deemed exempt.
   */
  public boolean isExemptTypeApproval(String typeApproval) {
    if (typeApproval == null || typeApproval.isEmpty()) {
      return false;
    }

    return this.exemptTypeApprovals.stream().anyMatch(typeApproval::equalsIgnoreCase);
  }
}
