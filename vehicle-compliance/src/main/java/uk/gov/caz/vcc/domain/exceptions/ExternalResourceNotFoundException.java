package uk.gov.caz.vcc.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception type to signify that a resource (typically served by an
 * external API) could not be located.
 *
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExternalResourceNotFoundException extends RuntimeException {

  public ExternalResourceNotFoundException(String message) {
    super(message);
  }
}
