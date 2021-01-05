package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception Internal exceptions will be thrown in hash
 * creation for password reset token.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AccountUserCodeHashCreationException extends ApplicationRuntimeException {
  public AccountUserCodeHashCreationException(String message) {
    super(message);
  }
}
