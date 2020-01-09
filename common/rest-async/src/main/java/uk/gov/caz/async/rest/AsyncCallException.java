package uk.gov.caz.async.rest;

/**
 * General async call Exception.
 */
public class AsyncCallException extends RuntimeException {

  /**
   * Constructor to create Exception with just an error message.
   *
   * @param message Error message of exception.
   */
  public AsyncCallException(String message) {
    super(message);
  }

  /**
   * Constructor to wrap Throwable into AsyncCallException.
   *
   * @param throwable Throwable to wrap.
   */
  public AsyncCallException(Throwable throwable) {
    super(throwable);
  }
}
