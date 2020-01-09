package uk.gov.caz.vcc.domain.exceptions;

/**
 * Custom exception to be thrown when a vehicle type cannot be determined.
 *
 */
public class UnidentifiableVehicleException extends RuntimeException {

  private static final long serialVersionUID = -3066077452827519338L;

  public UnidentifiableVehicleException(String errorMessage) {
    super(errorMessage);
  }
}
