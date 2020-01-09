package uk.gov.caz.taxiregister.service;

import java.util.List;
import java.util.Set;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.ValidationError;

/**
 * An interface which indicates the result of the registration run.
 */
public interface RegisterResult {

  /**
   * Check whether the registration has been successfully completed.
   *
   * @return true upon successful completion, false otherwise.
   */
  boolean isSuccess();

  /**
   * Gets all affected licensing authorities by the given registration run. Should return an empty
   * set upon failed run.
   *
   * @return A {@link Set} of {@link LicensingAuthority} which were affected by the given
   *     registration run.
   */
  Set<LicensingAuthority> getAffectedLicensingAuthorities();

  /**
   * Gets all validation errors which happened during the given registration run. An empty list is
   * returned upon successful run.
   *
   * @return A {@link List} of {@link ValidationError} which happened during the given registration
   *     run.
   */
  List<ValidationError> getValidationErrors();

}
