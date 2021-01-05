package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when user provided bad credentials or
 * account is locked.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UserLockoutException extends ApplicationRuntimeException {

  public UserLockoutException(String message) {
    super(message);
  }
}