package uk.gov.caz.vcc.dto.validation;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * Exception that indicates problems during parsing CAZ ID (usually UUID)
 */
@ResponseStatus(BAD_REQUEST)
public class InvalidCazZoneFormat extends RuntimeException {
  public InvalidCazZoneFormat(String cazId) {
    super(String.format("Invalid caz format [%s]", cazId));
  }
}
