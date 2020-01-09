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
 * A class which indicates the successful result of the registration run.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SuccessRegisterResult implements RegisterResult {

  Set<LicensingAuthority> affectedLicensingAuthorities;

  /**
   * See {@link RegisterResult#isSuccess()}.
   */
  public boolean isSuccess() {
    return true;
  }

  /**
   * See {@link RegisterResult#getAffectedLicensingAuthorities()}.
   */
  @Override
  public List<ValidationError> getValidationErrors() {
    return Collections.emptyList();
  }

  /**
   * Static factory method for {@link SuccessRegisterResult}.
   *
   * @param affectedLicensingAuthorities A set of licensing authorities affected by the
   *     registration run.
   * @return An instance of {@link SuccessRegisterResult} with {@code affectedLicensingAuthorities}.
   */
  public static SuccessRegisterResult with(Set<LicensingAuthority> affectedLicensingAuthorities) {
    return new SuccessRegisterResult(affectedLicensingAuthorities);
  }
}
