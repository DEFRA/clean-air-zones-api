package uk.gov.caz.vcc.domain;

import java.util.UUID;

/**
 * Object representation for charge calculation result.
 *
 */
public class  CalculationResult {

  private UUID cazIdentifier;

  private boolean exempt;

  private boolean compliant;

  private boolean chargeable;

  private float charge;

  public UUID getCazIdentifier() {
    return cazIdentifier;
  }

  public void setCazIdentifier(UUID cazIdentifier) {
    this.cazIdentifier = cazIdentifier;
  }

  public boolean getExempt() {
    return exempt;
  }

  public boolean getCompliant() {
    return compliant;
  }

  public void setCompliant(boolean compliant) {
    this.compliant = compliant;
  }

  public boolean getChargeable() {
    return chargeable;
  }

  public void setChargeable(boolean chargeable) {
    this.chargeable = chargeable;
  }

  public float getCharge() {
    return charge;
  }

  public void setCharge(float charge) {
    this.charge = charge;
  }

  public void setExempt(boolean exempt) {
    this.exempt = exempt;
  }
}
