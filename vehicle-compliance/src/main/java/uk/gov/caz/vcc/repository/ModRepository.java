package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import uk.gov.caz.vcc.domain.MilitaryVehicle;

/**
 * Repository for t_mod_whitelist.
 */
@Repository
public interface ModRepository extends CrudRepository<MilitaryVehicle, String> {

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
}