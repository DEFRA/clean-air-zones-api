package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not unique email was send to create
 * account.
 */
public class NotUniqueEmailException extends ApplicationRuntimeException {
  public NotUniqueEmailException(String message) {
    super(message);
  }
}