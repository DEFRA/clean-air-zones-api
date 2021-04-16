package uk.gov.caz.taxiregister.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequestParamsValidationException extends ApplicationRuntimeException {

  public RequestParamsValidationException(String message) {
    super(message);
  }
}
