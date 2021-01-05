package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Error response returned when account creation fails.
 */
@Value
@Builder
public class AccountError {
  String message;
  AccountErrorCode errorCode;

  public int getStatus() {
    return HttpStatus.UNPROCESSABLE_ENTITY.value();
  }
}
