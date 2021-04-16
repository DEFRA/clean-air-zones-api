package uk.gov.caz.taxiregister.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;

/**
 * A class that deals with looking up details of active licences for a given vehicle.
 */
@Service
@Slf4j
@AllArgsConstructor
public class LookupService {

  private final TaxiPhvLicencePostgresRepository licencesRepository;
  private final LicensingAuthorityPostgresRepository licensingAuthorityRepository;

  /**
   * Obtains information about any active operating licence and wheelchair accessibility for any
   * active operating licence for a given vrm.
   *
   * @param vrm VRM of a vehicle the licence information will be looked for.
   * @return {@link Optional#empty()} if vrm is absent in the database, {@link
   *     VehicleLicenceLookupInfo} wrapped in {@link Optional} otherwise.
   */
  @Cacheable(value = "vehicles", key = "#vrm")
  public Optional<VehicleLicenceLookupInfo> getLicenceInfoBy(String vrm) {
    List<TaxiPhvVehicleLicence> licences = licencesRepository.findByVrm(vrm);
    if (licences.isEmpty()) {
      log.info("No licences found in taxiPhvLicenceRepository for VRN");
      return Optional.empty();
    }
    List<String> licensingAuthorityNames = licensingAuthorityRepository
        .findAuthorityNamesByVrm(vrm);
    VehicleLicenceLookupInfo licenceLookupInfo = getVehicleLicenceLookupInfo(licences,
        licensingAuthorityNames);

    log.info("Vehicle licence lookup info for requested VRN '{}'", licenceLookupInfo);

    return Optional.of(licenceLookupInfo);
  }

  /**
   * Obtains information about any active operating licence and wheelchair accessibility for any
   * active operating licence for given vrms.
   *
   * @param vrms Set of VRMs of vehicles the licence information will be looked for.
   */
  public Map<String, VehicleLicenceLookupInfo> getLicencesInfoFor(Set<String> vrms) {
    Set<String> normalisedVrms = normaliseVrms(vrms);
    Map<String, List<TaxiPhvVehicleLicence>> allLicencesByVrm = licencesRepository.findByVrms(
        normalisedVrms);
    Map<String, List<String>> authorityNamesByVrm = licensingAuthorityRepository
        .findAuthorityNamesByVrms(normalisedVrms);

    return allLicencesByVrm.entrySet()
        .stream()
        .collect(
            toMap(
                Entry::getKey, // vrm
                e -> getVehicleLicenceLookupInfo(e.getValue(), authorityNamesByVrm.get(e.getKey()))
            )
        );
  }

  private VehicleLicenceLookupInfo getVehicleLicenceLookupInfo(List<TaxiPhvVehicleLicence> licences,
      List<String> licensingAuthorityNames) {
    boolean hasAnyLicenceActive = hasAnyOperatingLicenceActive(licences);
    String description = hasAnyDescription(licences);
    Boolean wheelchairAccessible = computeWheelchairAccessibleFlag(hasAnyLicenceActive, licences)
        .orElse(null);
    LocalDate licenceStatusExpirationDate = computeLicenceStatusExpirationDate(licences)
        .orElse(null);
    LocalDateTime licenceAddedTimestamp = computeLicenceAddedTimestamp(licences).orElse(null);

    return VehicleLicenceLookupInfo.builder()
        .hasAnyOperatingLicenceActive(hasAnyLicenceActive)
        .description(description)
        .wheelchairAccessible(wheelchairAccessible)
        .licensedStatusExpires(licenceStatusExpirationDate)
        .licensingAuthoritiesNames(licensingAuthorityNames)
        .addedTimestamp(licenceAddedTimestamp)
        .build();
  }

  private Optional<LocalDateTime> computeLicenceAddedTimestamp(
      List<TaxiPhvVehicleLicence> licences) {
    return licences.stream()
        .map(licence -> licence.getAddedTimestamp())
        .min(LocalDateTime::compareTo);
  }

  private Set<String> normaliseVrms(Set<String> vrms) {
    return vrms.stream()
        .map(this::normaliseVrm)
        .collect(Collectors.toSet());
  }

  private String normaliseVrm(String vrm) {
    return StringUtils.deleteWhitespace(vrm);
  }

  private boolean hasAnyOperatingLicenceActive(List<TaxiPhvVehicleLicence> licences) {
    Optional<TaxiPhvVehicleLicence> potentiallyActiveLicence = licences.stream()
        .filter(TaxiPhvVehicleLicence::isActive)
        .findFirst();
    return potentiallyActiveLicence.isPresent();
  }

  private String hasAnyDescription(List<TaxiPhvVehicleLicence> licences) {
    Optional<String> description = licences.stream()
        .filter(TaxiPhvVehicleLicence::isActive)
        .map(TaxiPhvVehicleLicence::getDescription)
        .findFirst();
    return description.orElse(null);
  }

  private Optional<Boolean> computeWheelchairAccessibleFlag(boolean hasAnyLicenceActive,
      List<TaxiPhvVehicleLicence> licences) {
    if (hasAnyLicenceActive) {
      return atLeastOneActiveLicenceIsWheelchairAccessible(licences);
    }

    // assertion: all licences are inactive
    if (allLicencesHaveNullWheelchairAccessibleFlag(licences)) {
      return Optional.empty();
    }
    return Optional.of(Boolean.FALSE);
  }

  private Optional<Boolean> atLeastOneActiveLicenceIsWheelchairAccessible(
      List<TaxiPhvVehicleLicence> licences) {
    List<Boolean> wheelchairAccessible = licences.stream()
        .filter(TaxiPhvVehicleLicence::isActive)
        .map(TaxiPhvVehicleLicence::getWheelchairAccessible)
        .filter(Objects::nonNull)
        .collect(toList());
    return wheelchairAccessible.isEmpty() ? Optional.empty()
        : Optional.of(wheelchairAccessible.contains(Boolean.TRUE));
  }

  private boolean allLicencesHaveNullWheelchairAccessibleFlag(
      List<TaxiPhvVehicleLicence> licences) {
    return licences.stream()
        .map(TaxiPhvVehicleLicence::getWheelchairAccessible)
        .allMatch(Objects::isNull);
  }

  private Optional<LocalDate> computeLicenceStatusExpirationDate(
      List<TaxiPhvVehicleLicence> licences) {
    return licences.stream()
        .filter(licence -> licence.isActive())
        .map(licence -> licence.getLicenseDates().getEnd())
        .max(LocalDate::compareTo);
  }
}
