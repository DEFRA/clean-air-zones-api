package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import uk.gov.caz.vcc.domain.RetrofittedVehicle;

/**
 * Repository for t_vehicle_retrofit.
 */
@Repository
public interface RetrofitRepository
    extends CrudRepository<RetrofittedVehicle, String> {

  /**
   * Check whether a vehicle is retrofitted by VRN.
   * 
   * @param vrn vehicle registration number of a vehicle
   * @return whether the vehicle exists in the retrofit table
   */
  Boolean existsByVrnIgnoreCase(String vrn);


  /**
   * Find retrofitted vehicle by VRN.
   *
   * @param vrn vehicle registration number of a vehicle
   * @return retrofitted vehicle
   */
  RetrofittedVehicle findByVrnIgnoreCase(String vrn);
}