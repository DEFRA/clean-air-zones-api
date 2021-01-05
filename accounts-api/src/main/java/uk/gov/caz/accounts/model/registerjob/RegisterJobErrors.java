package uk.gov.caz.accounts.model.registerjob;

import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * Wrapper class for a list of {@link RegisterJobError}s.
 */
@Value
public class RegisterJobErrors {
  @NonNull
  List<RegisterJobError> errors;

  public boolean hasErrors() {
    return !getErrors().isEmpty();
  }
}
