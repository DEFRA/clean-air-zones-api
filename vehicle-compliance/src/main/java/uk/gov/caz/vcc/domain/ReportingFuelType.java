package uk.gov.caz.vcc.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Entity which contains fuel type and corresponding Id for reporting.
 */
@Value
@Builder(toBuilder = true)
public class ReportingFuelType {
  
  UUID fuelTypeId;
  
  String fuelType;

}
