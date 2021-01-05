package uk.gov.caz.accounts.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailNotUniqueException extends ApplicationRuntimeException {

  public EmailNotUniqueException(String message) {
    super(message);
  }
}
