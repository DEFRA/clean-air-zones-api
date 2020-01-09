package uk.gov.caz.vcc.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExternalResourceNotFoundException extends RuntimeException {

  public ExternalResourceNotFoundException(String message) {
    super(message);
  }
}
