package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Error response returned when user login fails.
 */
@Value
@Builder
public class LoginError {

  String message;
  LoginErrorCode errorCode;

  public int getStatus() {
    return HttpStatus.UNAUTHORIZED.value();
  }
}
