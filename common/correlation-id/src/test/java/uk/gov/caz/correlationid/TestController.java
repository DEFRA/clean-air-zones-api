package uk.gov.caz.correlationid;


import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @RequestMapping("/test")
  public void defaultHandler() {
  }

  @RequestMapping("/test-with-response-body")
  public String defaultHandlerWithResponseBody() {
    return "test-with-response-body response";
  }

  @RequestMapping("/v1/test-with-response-body")
  public String v1Handler() {
    return "v1/test response";
  }

  @RequestMapping("/v1/test-without-response-body")
  public void v1VoidHandler() {
  }

  @RequestMapping("/customized/test")
  public void customizedHandler() {
  }

  @GetMapping("/v1/throw-exception")
  public void throwingExceptionHandler() {
    throw new CustomException();
  }

  @ExceptionHandler
  public String handle(CustomException e) {
    return "Achtung!";
  }

  private static class CustomException extends RuntimeException {

  }
}
