package uk.gov.caz;


import static org.springframework.http.HttpStatus.ACCEPTED;

import java.util.ArrayList;
import org.junit.platform.commons.util.Preconditions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/test")
public class TestController {

  public static final String CUSTOM_ERROR_MESSAGE = "This is custom message that should be visible to caller.";
  public static final HttpStatus ERROR_STATUS = ACCEPTED;

  @GetMapping("/standardMessage")
  public void standardMessageHandler() {
    //just make sure that any runtime exception is thrown
    Preconditions.notEmpty(new ArrayList<String>(), "");
  }

  @GetMapping("/customMessageWithoutReason")
  public void customMessageWithoutReasonHandler() {
    throw new ExceptionWithCustomResponseStatusWithoutReason(
        CUSTOM_ERROR_MESSAGE);
  }

  @GetMapping("/customMessageWithReason")
  public void customMessageWithReasonHandler() {
    throw new ExceptionWithCustomResponseStatusAndReason(
        CUSTOM_ERROR_MESSAGE);
  }

  @GetMapping("/responseStatusException")
  public void responseStatusExceptionThrower() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, CUSTOM_ERROR_MESSAGE);
  }

  @GetMapping("/customExceptionThrownWithCustomMessage")
  public void customExceptionThrownWithCustomMessage() {
    throw new TestException(CUSTOM_ERROR_MESSAGE);
  }

  @GetMapping("/customExceptionThrownWithCustomMessageAsReason")
  public void customExceptionThrownWithCustomMessageAsReason() {
    throw new TestExceptionWithReason("this is not default error message");
  }

  @ResponseStatus(ACCEPTED)
  public static class ExceptionWithCustomResponseStatusWithoutReason extends RuntimeException {

    ExceptionWithCustomResponseStatusWithoutReason(String message) {
      super(message);
    }
  }

  @ResponseStatus(value = ACCEPTED, reason = "Custom reason")
  public static class ExceptionWithCustomResponseStatusAndReason extends RuntimeException {

    ExceptionWithCustomResponseStatusAndReason(String message) {
      super(message);
    }
  }
}
