package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be thrown when user try to add the already existing account vehicle.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AccountVehicleAlreadyExistsException extends ApplicationRuntimeException {

  public AccountVehicleAlreadyExistsException() {
    super("AccountVehicle already exists");
  }
}
