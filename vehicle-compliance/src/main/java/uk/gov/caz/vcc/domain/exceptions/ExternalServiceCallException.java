package uk.gov.caz.vcc.domain.exceptions;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception type to signify that an exception was encountered when
 * attempting to communicate with an external resource (typically a REST API).
 *
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Service unavailable")
public class ExternalServiceCallException extends RuntimeException {

  private static final long serialVersionUID = -50430560923110204L;

  public ExternalServiceCallException(Throwable cause) {
    super("Service unavailable", cause);
  }

  public ExternalServiceCallException(Optional<String> message) {
    super(message.orElse("Service unavailable"));
  }

  public ExternalServiceCallException(String message) {
    super(message);
  }

  public ExternalServiceCallException() {
    this(Optional.empty());
  }
}
