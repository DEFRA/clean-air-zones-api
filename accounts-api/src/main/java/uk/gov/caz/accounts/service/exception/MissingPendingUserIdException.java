package uk.gov.caz.accounts.service.exception;

import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when user who tries to change his email has
 * no pendingUserId assigned.
 */
public class MissingPendingUserIdException extends ApplicationRuntimeException {
  public MissingPendingUserIdException(String message) {
    super(message);
  }
}
