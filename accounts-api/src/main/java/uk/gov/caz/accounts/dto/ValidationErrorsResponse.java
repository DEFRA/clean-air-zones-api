package uk.gov.caz.accounts.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.caz.accounts.controller.exception.ValidationError;

@Builder
@Getter
public class ValidationErrorsResponse {
  
  List<ValidationError> validationErrors;

  public static ValidationErrorsResponse from(List<ValidationError> errorsList) {
    return ValidationErrorsResponse.builder().validationErrors(errorsList).build();
  }
  
}
