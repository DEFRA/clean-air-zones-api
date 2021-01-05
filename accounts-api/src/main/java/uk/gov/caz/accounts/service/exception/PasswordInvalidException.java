package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when third-party service returns
 * information about invalid password.
 */
public class PasswordInvalidException extends ApplicationRuntimeException {
  public PasswordInvalidException(String message) {
    super(message);
  }
}
