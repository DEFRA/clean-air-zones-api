package uk.gov.caz.vcc.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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
  boolean existsByVrnIgnoreCase(String vrn);


  /**
   * Find retrofitted vehicle by VRN.
   *
   * @param vrn vehicle registration number of a vehicle
   * @return retrofitted vehicle
   */
  RetrofittedVehicle findByVrnIgnoreCase(String vrn);
  
  /**
   * Filter retrofitted vehicles from a given list of vrns.
   *
   * @param vrns list of vehicle registration number
   * @return a list of retrofitted vehicles
   */
  @Query(
      value = "SELECT * FROM t_vehicle_retrofit WHERE vrn IN :vrns",
      nativeQuery = true)
  List<RetrofittedVehicle> findRetrofitVehicleByVrns(@Param("vrns") Collection<String> vrns);
}