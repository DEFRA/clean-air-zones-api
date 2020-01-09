package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.caz.vcc.domain.ChargeValidity;

/**
 * Repository class that operates on {@link ChargeValidity}.
 */
public interface ChargeValidityRepository extends CrudRepository<ChargeValidity, String> {

}
