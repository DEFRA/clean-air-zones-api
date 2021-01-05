package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when token during password reset is not
 * valid or not active.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAccountUserPasswordResetCodeException extends ApplicationRuntimeException {

  public InvalidAccountUserPasswordResetCodeException() {
    super("Token is invalid or expired");
  }
}
