package uk.gov.caz.accounts.service.registerjob;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

/**
 * Value object to return result of Registered Job after processing.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegisterResult {

  private static final RegisterResult SUCCESS = new RegisterResult(Collections.emptyList());

  List<ValidationError> validationErrors;

  public boolean isSuccess() {
    return this == SUCCESS;
  }

  public static RegisterResult success() {
    return SUCCESS;
  }

  public static RegisterResult failure(List<ValidationError> validationErrors) {
    return new RegisterResult(validationErrors);
  }

  public static RegisterResult failure(ValidationError validationError) {
    return new RegisterResult(Collections.singletonList(validationError));
  }
}
