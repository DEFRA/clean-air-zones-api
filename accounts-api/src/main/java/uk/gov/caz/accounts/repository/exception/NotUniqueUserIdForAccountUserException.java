package uk.gov.caz.accounts.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class NotUniqueUserIdForAccountUserException extends ApplicationRuntimeException {
  public NotUniqueUserIdForAccountUserException(String message) {
    super(message);
  }
}
