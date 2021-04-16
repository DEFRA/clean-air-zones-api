package uk.gov.caz.vcc.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;

@Repository
public interface GeneralWhitelistRepository
    extends CrudRepository<GeneralWhitelistVehicle, String> {

  /**
   * Checks if a registration number appears on the general purpose whitelist.
   * 
   * @return {@link GeneralWhitelistVehicle} if on general whitelist, else null
   */
  Optional<GeneralWhitelistVehicle> findByVrnIgnoreCase(String vrn);

  /**
   * Filter GWL vehicles from a given list of vrns.
   *
   * @param vrns list of vehicle registration number
   * @return a list of GWL vehicles
   */
  @Query(
      value = "SELECT * FROM caz_whitelist_vehicles.t_whitelist_vehicles WHERE vrn IN :vrns",
      nativeQuery = true)
  List<GeneralWhitelistVehicle> findGwlVehiclesByVrns(@Param("vrns") Collection<String> vrns);
}
