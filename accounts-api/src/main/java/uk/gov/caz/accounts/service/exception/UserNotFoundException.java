package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when User is not found but was authorized
 * in Identity Provider.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UserNotFoundException extends ApplicationRuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }
}