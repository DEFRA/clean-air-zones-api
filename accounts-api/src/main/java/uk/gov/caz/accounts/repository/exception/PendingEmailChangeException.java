package uk.gov.caz.accounts.repository.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when provided invalid credentials.
 */
public class PendingEmailChangeException extends ApplicationRuntimeException {

  public PendingEmailChangeException(String message) {
    super(message);
  }
}
