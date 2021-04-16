package uk.gov.caz.vcc.repository;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.LocalVehicle;

@Repository
@ConditionalOnProperty(
    value = "services.remote-vehicle-data.use-remote-api", havingValue = "false",
    matchIfMissing = true)
public interface LocalVehicleDetailsRepository
    extends VehicleDetailsRepository, CrudRepository<LocalVehicle, String> {

  @Query(
      value = "SELECT new LocalVehicle("
          + "lv.registrationNumber, "
          + "lv.colour, "
          + "lv.dateOfFirstRegistration, "
          + "lv.euroStatus, "
          + "lv.typeApproval, "
          + "lv.massInService, "
          + "lv.bodyType, "
          + "lv.make, "
          + "lv.model, "
          + "lv.revenueWeight, "
          + "lv.seatingCapacity, "
          + "lv.standingCapacity, "
          + "lv.taxClass, "
          + "lv.fuelType) "
          + "FROM LocalVehicle lv "
          + "WHERE registrationnumber = TRIM(LEADING '0' from :vrn)"
  )
  Optional<Vehicle> findByRegistrationNumber(@Param("vrn") String registrationNumber);

}
