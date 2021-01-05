package uk.gov.caz.accounts.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.registerjob.RegisterJob;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;

/**
 * Database repository that manages {@link RegisterJob}s.
 */
@Repository
public interface RegisterJobRepository extends CrudRepository<RegisterJob, Integer> {
  long countAllByUploaderIdAndStatusIn(UUID uploaderId, Collection<RegisterJobStatus> status);

  Optional<RegisterJob> findByJobName(RegisterJobName jobName);
}
