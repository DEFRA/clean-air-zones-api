package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not unique account name was provided
 * during account creation.
 */
public class AccountAlreadyExistsException extends ApplicationRuntimeException {

  public AccountAlreadyExistsException(String message) {
    super(message);
  }
}
