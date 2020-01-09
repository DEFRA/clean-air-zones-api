package uk.gov.caz.taxiregister.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

  private final TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  private final LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  /**
   * Obtains information about any active operating licence and wheelchair accessibility for any
   * active operating licence for a given vrm.
   *
   * @param vrm VRM of a vehicle the licence information will be looked for.
   * @return {@link Optional#empty()} if vrm is absent in the database, {@link
   * VehicleLicenceLookupInfo} wrapped in {@link Optional} otherwise.
   */
  @Cacheable(value = "vehicles", key = "#vrm")
  public Optional<VehicleLicenceLookupInfo> getLicenceInfoBy(String vrm) {
    List<TaxiPhvVehicleLicence> licences = taxiPhvLicencePostgresRepository.findByVrm(vrm);

    if (licences.isEmpty()) {
      log.info("No licences found for '{}'", vrm);
      return Optional.empty();
    }

    List<String> licensingAuthorityNames = licensingAuthorityPostgresRepository
        .findAuthorityNamesByVrm(vrm);
    boolean hasAnyLicenceActive = hasAnyOperatingLicenceActive(licences);
    Boolean wheelchairAccessible = computeWheelchairAccessibleFlag(hasAnyLicenceActive, licences)
        .orElse(null);

    VehicleLicenceLookupInfo licenceLookupInfo = VehicleLicenceLookupInfo.builder()
        .hasAnyOperatingLicenceActive(hasAnyLicenceActive)
        .wheelchairAccessible(wheelchairAccessible)
        .licensingAuthoritiesNames(licensingAuthorityNames)
        .build();

    log.info("Vehicle licence lookup info for '{}': '{}'", vrm, licenceLookupInfo);

    return Optional.of(licenceLookupInfo);
  }

  private boolean hasAnyOperatingLicenceActive(List<TaxiPhvVehicleLicence> licences) {
    Optional<TaxiPhvVehicleLicence> potentiallyActiveLicence = licences.stream()
        .filter(TaxiPhvVehicleLicence::isActive)
        .findFirst();
    return potentiallyActiveLicence.isPresent();
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
}
