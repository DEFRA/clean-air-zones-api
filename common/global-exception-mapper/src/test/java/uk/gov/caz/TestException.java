package uk.gov.caz;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
public class TestException extends ApplicationRuntimeException {
  public TestException(String message) {
    super(message);
  }
}
