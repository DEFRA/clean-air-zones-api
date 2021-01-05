package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.accounts.model.AccountUserCode;

/**
 * Exception class which will be used to throw exception when {@link AccountUserCode} cannot be
 * found while getting user from DB after successful validation.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AccountUserCodeNotFoundException extends ApplicationRuntimeException {
  public AccountUserCodeNotFoundException(String message) {
    super(message);
  }
}
