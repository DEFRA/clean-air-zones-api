package uk.gov.caz.accounts.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.ProhibitedTerm;

/**
 * A default CRUD repository for prohibited terms.
 */
@Repository
public interface ProhibitedTermRepository extends CrudRepository<ProhibitedTerm, Long> {

}
