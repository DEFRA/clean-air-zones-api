package uk.gov.caz.definitions.exceptions;

/**
 * Custom exception to be thrown when an unexpected fuel type is received.
 */
public class UnrecognisedFuelTypeException extends IllegalArgumentException {

  private static final long serialVersionUID = -5470694172479487808L;

  public UnrecognisedFuelTypeException(String errorMessage) {
    super(errorMessage);
  }

}
