package uk.gov.caz.vcc.repository;

import java.util.Map;
import java.util.Set;
import uk.gov.caz.vcc.domain.MilitaryVehicle;

public interface ModDataProvider {
  /**
   * Check that a military vehicle exists by VRN.
   *
   * @param vrn vehicle registration number of a vehicle
   * @return whether vehicle exists in the mod whitelist table
   */
  Boolean existsByVrnIgnoreCase(String vrn);

  /**
   * Find military vehicle by VRN.
   *
   * @param vrn vehicle registration number of a vehicle
   * @return military vehicle
   */
  MilitaryVehicle findByVrnIgnoreCase(String vrn);

  /**
   * Check that military vehicles exist by VRN.
   *
   * @param vrns List of vehicle registration numbers
   * @return a {@link Map} that maps a vrn to a boolean value that tells whether the vehicle
   *     identified by that vrn is a military one.
   */
  Map<String, Boolean> existByVrns(Set<String> vrns);
}