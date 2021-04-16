package uk.gov.caz.whitelist.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception which is thrown when trying to whitelist already whitelisted VRN.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class VrnAlreadyWhitelistedException extends ApplicationRuntimeException {

  public VrnAlreadyWhitelistedException(String message) {
    super(message);
  }
}
