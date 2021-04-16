package uk.gov.caz.vcc.domain.service.compliance;

import uk.gov.caz.definitions.domain.Vehicle;

/**
 * Interface definition for Clean Air Zone compliance services.
 *
 */
public interface CazComplianceService {
  boolean isVehicleCompliant(Vehicle vehicle);
}
