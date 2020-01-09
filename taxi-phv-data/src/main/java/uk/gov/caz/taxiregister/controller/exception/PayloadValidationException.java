package uk.gov.caz.taxiregister.controller.exception;

public class PayloadValidationException extends RuntimeException {

  public PayloadValidationException(String message) {
    super(message);
  }
}