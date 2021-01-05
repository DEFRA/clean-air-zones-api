package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when the provided user-verification token
 * is expired.
 */
public class ExpiredUserEmailVerificationCodeException extends ApplicationRuntimeException {

  public ExpiredUserEmailVerificationCodeException() {
    super("Expired token");
  }
}