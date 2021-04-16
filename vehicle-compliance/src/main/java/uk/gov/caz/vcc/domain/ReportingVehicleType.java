package uk.gov.caz.vcc.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Entity which contains vehicle type and corresponding Id for reporting.
 */
@Value
@Builder(toBuilder = true)
public class ReportingVehicleType {
  
  UUID vehicleTypeId;
  
  String vehicleType;

}
