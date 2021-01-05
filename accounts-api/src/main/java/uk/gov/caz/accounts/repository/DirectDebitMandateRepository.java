package uk.gov.caz.accounts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import uk.gov.caz.accounts.model.DirectDebitMandate;

public interface DirectDebitMandateRepository extends CrudRepository<DirectDebitMandate, String> {

  List<DirectDebitMandate> findAllByAccountId(UUID accountId);

  Optional<DirectDebitMandate> findByPaymentProviderMandateId(String paymentProviderId);

  Optional<DirectDebitMandate> findById(UUID id);
}
