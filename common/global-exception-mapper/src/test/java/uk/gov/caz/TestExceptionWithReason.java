package uk.gov.caz;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = TestController.CUSTOM_ERROR_MESSAGE)
public class TestExceptionWithReason extends ApplicationRuntimeException {

  public TestExceptionWithReason(String message) {
    super(message);
  }
}
