package uk.gov.caz.taxiregister.service;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;

/**
 * This service provides security checks necessary for licences registration operation. It must be
 * invoked for both API and CSV registration flow and it is mandatory to stop/do not run
 * registration job if any check fails.
 */
@Service
@AllArgsConstructor
public class LicencesRegistrationSecuritySentinel {

  private final LicensingAuthorityPostgresRepository repository;

  /**
   * Each Uploader has specific permissions to modify licences of only selected Licensing
   * Authorities. This method validates if incoming registration data, sent by particular Uploader,
   * is not violating these permissions.
   *
   * @param uploaderId {@link UUID} that identifies Uploader trying to modify licences.
   * @param licensingAuthoritiesThisUploaderWantsToModify A set of Licensing Authorities for
   *     which Uploader wants to modify licences. If this set is empty, method will return
   *     Optional.empty() meaning that security check passed - nothing to modify.
   * @return An {@link Optional} of {@link String} - if it is empty: Uploader has permissions to
   *     modify all it wants to modify. Not empty: It contains error message stating current
   *     permissions of Uploader. Further processing/registration should stop if this is not empty.
   */
  public Optional<String> checkUploaderPermissionsToModifyLicensingAuthorities(
      UUID uploaderId, Set<LicensingAuthority> licensingAuthoritiesThisUploaderWantsToModify) {
    if (licensingAuthoritiesThisUploaderWantsToModify.isEmpty()) {
      return Optional.empty();
    }
    Set<String> hasPermissionToModify =
        getLicensingAuthorityNamesThatThisUploaderHasPermissionToModify(uploaderId);

    Set<String> wantsToModify =
        getLicensingAuthorityNamesThatThisUploaderWantsToModify(
            licensingAuthoritiesThisUploaderWantsToModify);

    SetView<String> noPermissionToModify = Sets.difference(wantsToModify, hasPermissionToModify);

    if (noPermissionToModify.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(prepareErrorMessage(noPermissionToModify));
  }

  /**
   * Queries database and finds out all names of Licensing Authorities that Uploader has permissions
   * to modify.
   *
   * @param uploaderId {@link UUID} that identifies Uploader making a request.
   * @return Set of names of Licensing Authorities that this Uploader has permissions to modify.
   */
  private Set<String> getLicensingAuthorityNamesThatThisUploaderHasPermissionToModify(
      UUID uploaderId) {
    return collectNamesToSet(repository.findAllowedToBeModifiedBy(uploaderId));
  }

  /**
   * Given Set of {@link LicensingAuthority} extracts their names and puts them in Set.
   *
   * @param licensingAuthoritiesThisUploaderWantsToModify Set of {@link LicensingAuthority}.
   * @return Set of names from input Set.
   */
  private Set<String> getLicensingAuthorityNamesThatThisUploaderWantsToModify(
      Set<LicensingAuthority> licensingAuthoritiesThisUploaderWantsToModify) {
    return collectNamesToSet(licensingAuthoritiesThisUploaderWantsToModify);
  }

  /**
   * Given Collection of {@link LicensingAuthority} extracts their names and puts them in Set.
   *
   * @param licensingAuthorityStream Collection of {@link LicensingAuthority}.
   * @return Set of names from input Collection.
   */
  private Set<String> collectNamesToSet(Collection<LicensingAuthority> licensingAuthorityStream) {
    return licensingAuthorityStream
        .stream()
        .map(LicensingAuthority::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Given Set of Licensing Authority names that this Uploader is not allowed to modify, prepares
   * error message.
   *
   * @param notAllowedToBeModifiedByUploader Set of Licensing Authority names that this Uploader
   *     is not allowed to modify.
   * @return Error message stating list of Licensing Authority names for which Uploader does not
   *     have permissions.
   */
  private String prepareErrorMessage(Set<String> notAllowedToBeModifiedByUploader) {
    String licensingAuthorities = join(notAllowedToBeModifiedByUploader);
    return "You are not authorised to submit data for " + licensingAuthorities;
  }

  /**
   * Joins Licensing Authority names by comma.
   *
   * @param notAllowedToBeModifiedByUploader Set of Licensing Authority names to be joined.
   * @return Joined Licensing Authority names.
   */
  private String join(Set<String> notAllowedToBeModifiedByUploader) {
    return String.join(", ", notAllowedToBeModifiedByUploader);
  }
}
