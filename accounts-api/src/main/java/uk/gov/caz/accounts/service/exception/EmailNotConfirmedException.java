package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when has not verified email but want to
 * login to the app.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class EmailNotConfirmedException extends ApplicationRuntimeException {

  public EmailNotConfirmedException() {
    super("Email not confirmed");
  }
}
