package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when a passsword was used recently.
 */
public class PasswordRecentlyUsedException extends ApplicationRuntimeException {
  public PasswordRecentlyUsedException(String message) {
    super(message);
  }
}
