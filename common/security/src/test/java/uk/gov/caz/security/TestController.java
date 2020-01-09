package uk.gov.caz.security;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @RequestMapping("/test-ok")
  public void defaultHandler() {
  }

  @RequestMapping("/nested/test-ok")
  public void defaultNestedHandler() {
  }

  @GetMapping("/throw-exception")
  public void throwingExceptionHandler() {
    throw new CustomException();
  }

  @GetMapping("/test-404")
  public ResponseEntity returning404() {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler
  public String handle(CustomException e) {
    return "Alarm!";
  }

  private static class CustomException extends RuntimeException {

  }
}
