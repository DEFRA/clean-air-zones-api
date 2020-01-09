package uk.gov.caz.taxiregister.controller.exception;

public class InvalidUploaderIdFormatException extends IllegalArgumentException {

  public InvalidUploaderIdFormatException(String message) {
    super(message);
  }
}