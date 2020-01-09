package uk.gov.caz.vcc.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.vcc.domain.Vehicle;

@Repository
@ConditionalOnProperty(
    value = "services.remote-vehicle-data.use-remote-api", havingValue = "false",
    matchIfMissing = true)
public interface LocalVehicleDetailsRepository
    extends VehicleDetailsRepository, CrudRepository<Vehicle, String> {
}
