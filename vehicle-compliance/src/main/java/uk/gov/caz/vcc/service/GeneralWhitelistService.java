package uk.gov.caz.vcc.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;

@Slf4j
@AllArgsConstructor
@Service
public class GeneralWhitelistService {

  public static final String COMPLIANT = "compliant";
  public static final String EXEMPTED = "exempted";
  private final GeneralWhitelistRepository generalWhitelistRepository;

  /**
   * Tries to find vehicle with VRN in General Whitelist database.
   *
   * @param vrn VRN of vehicle to find.
   * @return {@link Optional} of {@link GeneralWhitelistVehicle} - it will be empty if vehicle with
   *     specified VRN is not in General Whitelist database.
   */
  public Optional<GeneralWhitelistVehicle> tryToFindFor(String vrn) {
    return generalWhitelistRepository.findByVrnIgnoreCase(vrn);
  }

  public List<GeneralWhitelistVehicle> findByVrns(Set<String> vrns) {
    return generalWhitelistRepository.findGwlVehiclesByVrns(vrns);
  }

  /**
   * Indicates if a VRN is present and exempt on the general purpose whitelist.
   *
   * @return {@code true} if exempt on general whitelist, else {@code false}
   */
  public boolean exemptOnGeneralWhitelist(String vrn) {
    Optional<GeneralWhitelistVehicle> vehicle = generalWhitelistRepository.findByVrnIgnoreCase(vrn);
    return vehicle.isPresent() && vehicle.get().isExempt();
  }

  /**
   * If a VRN is both present on the whitelist and exempt, returns the category of exemption.
   *
   * @param vrn the vehicle registration number
   * @return Exemption category if present
   */
  public Optional<String> getExemptionCategory(String vrn) {
    Optional<GeneralWhitelistVehicle> vehicle = generalWhitelistRepository.findByVrnIgnoreCase(vrn);
    if (vehicle.isPresent() && vehicle.get().isExempt()) {
      return Optional.of(vehicle.get().getCategory());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Indicates if a VRN is present and compliant on the general purpose whitelist.
   *
   * @return {@code true} if compliant on general whitelist, else {@code false}
   */
  public boolean compliantOnGeneralWhitelist(String vrn) {
    Optional<GeneralWhitelistVehicle> vehicle = generalWhitelistRepository.findByVrnIgnoreCase(vrn);
    return vehicle.isPresent() && vehicle.get().isCompliant();
  }

  /**
   * Checks whether vehicle is present on General Purpose Whitelist and is categorized as "Non-UK
   * Vehicle" based on pre-fetched results.
   */
  public boolean isOnGeneralPurposeWhitelistAndHasCategoryNonUk(String vrn,
      List<GeneralWhitelistVehicle> matchedGpwVehicles) {
    return matchedGpwVehicles.stream()
        .anyMatch(gpwVehicle -> gpwVehicle.getVrn().equals(vrn)
            && hasGeneralPurposeCategoryNonUk(gpwVehicle));
  }

  /**
   * Checks whether vehicle is present of General Purpose Whitelist and is categorized as "Other"
   * based on pre-fetched results.
   */
  public boolean isOnGeneralPurposedWhitelistAndHasCategoryOther(String vrn,
      List<GeneralWhitelistVehicle> matchedGpwVehicles) {
    return matchedGpwVehicles.stream()
        .anyMatch(gpwVehicle -> gpwVehicle.getVrn().equals(vrn)
            && hasGeneralPurposeCategoryOther(gpwVehicle));
  }

  /**
   * Checks whether vehicle is present on General Purpose Whitelist and is categorized as
   * "Problematic VRN".
   */
  public boolean isOnGeneralPurposeWhitelistAndHasCategoryProblematicVrn(
      String vrn, List<GeneralWhitelistVehicle> matchedGpwVehicles) {
    // Stream result from in-memory list
    return matchedGpwVehicles.stream()
        .anyMatch(gpwVehicle -> gpwVehicle.getVrn().equals(vrn)
            && hasGeneralPurposeCategoryProblematicVrn(gpwVehicle));
  }

  /**
   * Checks whether vehicle has General Purpose Whitelist vehicle has category "Problematic VRN".
   */
  private static Boolean hasGeneralPurposeCategoryProblematicVrn(
      GeneralWhitelistVehicle gpwlVehicle) {
    return gpwlVehicle.getCategory().equalsIgnoreCase("Problematic VRN");
  }

  /**
   * Checks whether vehicle has General Purpose Whitelist vehicle has category "Non-UK Vehicle".
   */
  private static Boolean hasGeneralPurposeCategoryNonUk(
      GeneralWhitelistVehicle gpwlVehicle) {
    return gpwlVehicle.getCategory().equalsIgnoreCase("Non-UK Vehicle");
  }

  /**
   * Checks whether vehicle has General Purpose Whitelist vehicle category "Other".
   */
  private static Boolean hasGeneralPurposeCategoryOther(
      GeneralWhitelistVehicle gpwlVehicle) {
    return gpwlVehicle.getCategory().equalsIgnoreCase("Other");
  }

  /**
   * Checks if a vehicle is exempt by virtue of its category on the General Purpose Whitelist.
   */
  public boolean isExemptOnGeneralPurposeWhitelist(String vrn,
      List<GeneralWhitelistVehicle> matchedGpwVehicles) {
    return matchedGpwVehicles.stream()
        .anyMatch(gpwVehicle -> gpwVehicle.getVrn().equals(vrn)
            && gpwVehicle.isExempt());
  }

  /**
   * Checks if a vehicle is compliant by virtue of its category on the General Purpose Whitelist.
   */
  public boolean isCompliantOnGeneralPurposeWhitelist(String vrn,
      List<GeneralWhitelistVehicle> matchedGpwVehicles) {
    return matchedGpwVehicles.stream()
        .anyMatch(gpwVehicle -> gpwVehicle.getVrn().equals(vrn)
            && gpwVehicle.isCompliant());
  }
}