package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when the provided user-verification token
 * is invalid, i.e. there is no account associated with it or the code is absent in the database.
 */
public class InvalidUserEmailVerificationCodeException extends ApplicationRuntimeException {

  public InvalidUserEmailVerificationCodeException() {
    super("Invalid token");
  }
}