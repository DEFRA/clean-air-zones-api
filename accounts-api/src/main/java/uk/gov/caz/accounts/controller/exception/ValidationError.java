package uk.gov.caz.accounts.controller.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class ValidationError {

  private String message;
  private static final String BAD_PARAMETER_EXCEPTION = " must be correctly provided";
  
  public static ValidationError from(String queryString) {
    return ValidationError.builder().message(queryString + BAD_PARAMETER_EXCEPTION).build(); 
  }
  
}
