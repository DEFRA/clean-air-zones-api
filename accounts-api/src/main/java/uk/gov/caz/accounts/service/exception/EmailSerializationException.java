package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception the data required for password reset cannot
 * be serialized.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailSerializationException extends ApplicationRuntimeException {

  public EmailSerializationException(String message) {
    super(message);
  }
}
