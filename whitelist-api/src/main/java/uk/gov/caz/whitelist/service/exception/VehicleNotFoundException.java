package uk.gov.caz.whitelist.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * An exception thrown when a vehicle that is supposed to be deleted is not found in the database.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class VehicleNotFoundException extends ApplicationRuntimeException {

  public VehicleNotFoundException() {
    super("Vehicle not found");
  }
}
