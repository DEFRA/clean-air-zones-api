package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not able to find vrn in DB.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class VrnNotFoundException extends ApplicationRuntimeException {

  private static final long serialVersionUID = 3123693872440762004L;

  public VrnNotFoundException(String message) {
    super(message);
  }

}
