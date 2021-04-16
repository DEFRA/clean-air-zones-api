package uk.gov.caz.vcc.domain.exceptions;

/**
 * A custom exception type used when a vehicle's compliance
 * could not be determined (due to not having a business rule path).
 *
 */
public class UnableToIdentifyVehicleComplianceException extends RuntimeException {
  public UnableToIdentifyVehicleComplianceException(String message) {
    super(message);
  }

  private static final long serialVersionUID = -3827712051005503818L;
}