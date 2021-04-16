package uk.gov.caz.vcc.repository;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import uk.gov.caz.vcc.domain.VehicleEntrantReporting;

/**
 * Repository class that operates on {@link VehicleEntrantReporting}.
 */
public interface VehicleEntrantReportingRepository extends
    CrudRepository<VehicleEntrantReporting, UUID> {

}
