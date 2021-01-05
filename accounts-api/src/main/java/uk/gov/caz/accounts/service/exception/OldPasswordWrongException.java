package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when old password is wrong.
 */
public class OldPasswordWrongException extends ApplicationRuntimeException {
  public OldPasswordWrongException(String message) {
    super(message);
  }
}
