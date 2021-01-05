package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not able to find the owning user in
 * DB.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvitingUserNotFoundException extends ApplicationRuntimeException {

  private static final long serialVersionUID = -8174186307617154888L;

  public InvitingUserNotFoundException(String message) {
    super(message);
  }
}
