package uk.gov.caz.vcc.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Service unavailable")
public class ExternalServiceCallException extends RuntimeException {
  public ExternalServiceCallException(Throwable cause) {
    super("Service unavailable", cause);
  }
  public ExternalServiceCallException() {
    super("Service unavailable");
  }
}
