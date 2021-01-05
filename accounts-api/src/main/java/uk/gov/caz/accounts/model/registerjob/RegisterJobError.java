package uk.gov.caz.accounts.model.registerjob;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Value;

/**
 * An error that may happen when processing the job.
 */
@Value
@JsonInclude(Include.NON_EMPTY)
public class RegisterJobError {

  String vrn;
  String title;
  String detail;

  /**
   * Creates an instance of {@link RegisterJobError} based on of an instance of {@link
   * ValidationError}.
   */
  public static RegisterJobError from(ValidationError validationError) {
    return new RegisterJobError(
        validationError.getVrn(),
        validationError.getTitle(),
        validationError.getDetail()
    );
  }

  public static RegisterJobError withDetailOnly(String detail) {
    return new RegisterJobError(null, null, detail);
  }
}
