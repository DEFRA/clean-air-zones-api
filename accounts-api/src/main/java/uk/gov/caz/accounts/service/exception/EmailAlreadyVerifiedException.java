package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when an attempt is made to verify
 * a user which had already been verified.
 */
public class EmailAlreadyVerifiedException extends ApplicationRuntimeException {

  public EmailAlreadyVerifiedException() {
    super("Email already verified");
  }
}
