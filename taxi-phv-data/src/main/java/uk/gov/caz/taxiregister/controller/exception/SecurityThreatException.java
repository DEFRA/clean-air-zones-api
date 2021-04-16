package uk.gov.caz.taxiregister.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception thrown, when an attacker is trying to inject html code into text fields.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class SecurityThreatException extends ApplicationRuntimeException {

  public SecurityThreatException(String message) {
    super(message);
  }
}
