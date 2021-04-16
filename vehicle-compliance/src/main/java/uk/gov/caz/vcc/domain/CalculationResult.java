package uk.gov.caz.vcc.domain;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Object representation for charge calculation result.
 *
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class CalculationResult {

  private UUID cazIdentifier;

  private boolean exempt;

  private Boolean compliant;

  private boolean chargeable;

  private float charge;
  
  private Boolean isRetrofitted;
  
  public void unableToIdentifyVehicleCompliant() {
    this.compliant = null;
  }
}
