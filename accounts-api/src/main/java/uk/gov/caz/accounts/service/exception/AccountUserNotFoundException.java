package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not able to find account in DB.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountUserNotFoundException extends ApplicationRuntimeException {

  public AccountUserNotFoundException(String message) {
    super(message);
  }
}
