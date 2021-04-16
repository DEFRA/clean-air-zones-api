package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.caz.vcc.domain.EntrantExemption;

/**
 * Repository class that operates on {@link EntrantExemption}.
 */
public interface EntrantExemptionRepository extends CrudRepository<EntrantExemption, String> {

}
