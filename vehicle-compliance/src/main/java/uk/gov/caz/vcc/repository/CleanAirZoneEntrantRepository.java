package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;

/**
 * Repository class that operates on {@link CleanAirZoneEntrant}.
 */
public interface CleanAirZoneEntrantRepository extends
    CrudRepository<CleanAirZoneEntrant, Integer> {

}
