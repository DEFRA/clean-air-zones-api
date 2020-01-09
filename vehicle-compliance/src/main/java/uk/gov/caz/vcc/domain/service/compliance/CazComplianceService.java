package uk.gov.caz.vcc.domain.service.compliance;

import uk.gov.caz.vcc.domain.Vehicle;

public interface CazComplianceService {
  boolean isVehicleCompliance(Vehicle vehicle);
}