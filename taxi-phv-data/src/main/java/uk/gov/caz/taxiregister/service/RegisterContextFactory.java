package uk.gov.caz.taxiregister.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.service.RegisterService.RegisterContext;

@Component
@Slf4j
@AllArgsConstructor
public class RegisterContextFactory {

  private final LicensingAuthorityPostgresRepository licensingAuthorityRepository;
  private final TaxiPhvLicencePostgresRepository vehicleRepository;

  /**
   * Creates an instance of {@link RegisterContext}, a helper object which is used by {@link
   * RegisterService}.
   *
   * @param licences A set of licence which are to be registered.
   * @param uploaderId An ID of the entity which registers the licences.
   * @return An instance of {@link RegisterContext}.
   * @throws LicensingAuthorityMismatchException if {@code} licences contains a licence whose
   *     licensing authority is absent in the database.
   */
  public RegisterService.RegisterContext createContext(Set<TaxiPhvVehicleLicence> licences,
      UUID uploaderId) {
    Map<String, LicensingAuthority> currentLicensingAuthoritiesByName =
        licensingAuthorityRepository.findAll();

    checkMatchingLicenceAuthorityPrecondition(licences, currentLicensingAuthoritiesByName);

    Map<LicensingAuthority, Set<TaxiPhvVehicleLicence>> newLicencesByLicensingAuthority =
        groupByLicensingAuthority(licences, currentLicensingAuthoritiesByName);

    return new RegisterService.RegisterContext(
        uploaderId,
        newLicencesByLicensingAuthority,
        byUniqueAttr(newLicencesByLicensingAuthority.keySet()),
        currentLicensingAuthoritiesByName
    );
  }

  /**
   * Transforms an input set to a map whose keys are elements of this set and values are maps which
   * pairs the unique attributes of the given licence with the licence.
   *
   * @param licensingAuthorities A set of {@link LicensingAuthority}.
   * @return A mapping between {@link LicensingAuthority} and pairs of licences and their unique
   *     attributes represented as a {@link Map}.
   */
  private Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>> byUniqueAttr(
      Set<LicensingAuthority> licensingAuthorities) {
    return licensingAuthorities
        .stream()
        .map(LicensingAuthority::getId)
        .map(vehicleRepository::findByLicensingAuthority)
        .flatMap(Set::stream)
        .collect(Collectors.groupingBy(TaxiPhvVehicleLicence::getLicensingAuthority,
            Collectors.toMap(UniqueLicenceAttributes::from, Function.identity())));
  }

  /**
   * Checks whether every element of the input set of {@link TaxiPhvVehicleLicence} has a licensing
   * authority which is present in the database.
   *
   * @param licences A set of licences to check.
   * @param licensingAuthoritiesByName A map of data from database. It maps licence's authority
   *     name to a given licence authority.
   * @throws LicensingAuthorityMismatchException if {@code licences} contains elements whose
   *     licensing authority does not exist in the database.
   */
  private void checkMatchingLicenceAuthorityPrecondition(Set<TaxiPhvVehicleLicence> licences,
      Map<String, LicensingAuthority> licensingAuthoritiesByName) {
    List<TaxiPhvVehicleLicence> nonMatchingLicences = licences.stream()
        .filter(licence -> !matchesExistingLicensingAuthority(licence, licensingAuthoritiesByName))
        .collect(Collectors.toList());

    if (!nonMatchingLicences.isEmpty()) {
      throw new LicensingAuthorityMismatchException(nonMatchingLicences);
    }
  }

  /**
   * For every licence in {@code newLicences} updates its licensing authority with the one from the
   * database and groups it by licensing authority into sets.
   *
   * @param newLicences A set of {@link TaxiPhvVehicleLicence} which is to be processed.
   * @param currentLicensingAuthoritiesByName Licensing authorities present in the database.
   * @return A map whose keys are licensing authorities and their licences are values.
   */
  private Map<LicensingAuthority, Set<TaxiPhvVehicleLicence>> groupByLicensingAuthority(
      Set<TaxiPhvVehicleLicence> newLicences,
      Map<String, LicensingAuthority> currentLicensingAuthoritiesByName) {
    return newLicences.stream()
        .map(licence ->
            createWithMatchingLicensingAuthority(currentLicensingAuthoritiesByName, licence))
        .collect(Collectors.groupingBy(TaxiPhvVehicleLicence::getLicensingAuthority,
            Collectors.toSet()));
  }

  /**
   * Checks if {@code licence}'s authority name exists (as a key) in {@code
   * licensingAuthorityByName}.
   *
   * @param licence A licence whose licence authority presence will be checked.
   * @param licensingAuthorityByName A map containing licensing authorities.
   * @return true if {@code licensingAuthorityByName} contains {@code licence}'s authority name (as
   *     a key), false otherwise.
   */
  private boolean matchesExistingLicensingAuthority(TaxiPhvVehicleLicence licence,
      Map<String, LicensingAuthority> licensingAuthorityByName) {
    String licensingAuthorityName = licence.getLicensingAuthority().getName();
    boolean matches = licensingAuthorityByName.containsKey(licensingAuthorityName);
    if (!matches) {
      log.warn("Licensing authority '{}' does not exist in the database", licensingAuthorityName);
    }
    return matches;
  }

  /**
   * Transforms {@code licence} into another licence containing matching licensing authority from
   * the database.
   *
   * @param currentLicensingAuthoritiesByName A map containing licensing authorities with all
   *     attributes set (name and id)
   * @param licence A licence whose licensing authority possibly does not contain
   *     database-related attributes like {@code id}.
   * @return An copy of {@code licence} which contains the matching licensing authority from the
   *     database.
   */
  private TaxiPhvVehicleLicence createWithMatchingLicensingAuthority(
      Map<String, LicensingAuthority> currentLicensingAuthoritiesByName,
      TaxiPhvVehicleLicence licence) {
    String licensingAuthorityName = licence.getLicensingAuthority().getName();
    return licence.toBuilder()
        .licensingAuthority(currentLicensingAuthoritiesByName.get(licensingAuthorityName))
        .build();
  }
}
