package uk.gov.caz.taxiregister.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.ValidationError;

/**
 * A class which indicates the failed result of the registration run.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FailureRegisterResult implements RegisterResult {

  List<ValidationError> validationErrors;

  /**
   * See {@link RegisterResult#isSuccess()}.
   */
  public boolean isSuccess() {
    return false;
  }

  /**
   * See {@link RegisterResult#getAffectedLicensingAuthorities()}.
   */
  @Override
  public Set<LicensingAuthority> getAffectedLicensingAuthorities() {
    return Collections.emptySet();
  }

  /**
   * Static factory method for {@link FailureRegisterResult}.
   *
   * @param validationErrors A list of validation errors which happened during the registration
   *     run.
   * @return An instance of {@link FailureRegisterResult} with {@code validationErrors}.
   */
  public static FailureRegisterResult with(List<ValidationError> validationErrors) {
    return new FailureRegisterResult(validationErrors);
  }

  /**
   * Static factory method for {@link FailureRegisterResult}.
   *
   * @param validationError A validation error which happened during the registration run.
   * @return An instance of {@link FailureRegisterResult} with {@code validationError}.
   */
  public static FailureRegisterResult with(ValidationError validationError) {
    return new FailureRegisterResult(Collections.singletonList(validationError));
  }
}
