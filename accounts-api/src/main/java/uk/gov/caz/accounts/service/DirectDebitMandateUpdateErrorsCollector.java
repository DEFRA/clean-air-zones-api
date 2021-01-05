package uk.gov.caz.accounts.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.DirectDebitMandateUpdateError;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;

/**
 * Class responsible for collecting errors for provided updates of direct debit mandates.
 */
@Component
@AllArgsConstructor
public class DirectDebitMandateUpdateErrorsCollector {

  private final DirectDebitMandateRepository directDebitMandateRepository;

  /**
   * Method collects errors from a given list of updates and returns them as a collection.
   *
   * @param accountId ID of the account.
   * @param singleDirectDebitMandateUpdates list of updates for direct debit mandates.
   * @return list of errors.
   */
  public List<DirectDebitMandateUpdateError> collectErrors(UUID accountId,
      List<SingleDirectDebitMandateUpdate> singleDirectDebitMandateUpdates) {
    List<DirectDebitMandateUpdateError> errors = new ArrayList<>();

    for (SingleDirectDebitMandateUpdate singleUpdate : singleDirectDebitMandateUpdates) {
      errors.addAll(detectErrorsForSingleUpdate(accountId, singleUpdate));
    }

    return errors;
  }

  /**
   * Collects errors for a single mandate update.
   *
   * @param accountId ID of the account.
   * @param singleUpdate data of the single mandate to update.
   * @return List of errors for a single mandate.
   */
  private List<DirectDebitMandateUpdateError> detectErrorsForSingleUpdate(UUID accountId,
      SingleDirectDebitMandateUpdate singleUpdate) {
    List<DirectDebitMandateUpdateError> errors = new ArrayList<>();

    if (!EnumUtils.isValidEnum(DirectDebitMandateStatus.class, singleUpdate.getStatus())) {
      errors.add(DirectDebitMandateUpdateError.invalidMandateStatus(singleUpdate.getMandateId()));
    }

    Optional<DirectDebitMandate> directDebitMandateToVerify = directDebitMandateRepository
        .findByPaymentProviderMandateId(singleUpdate.getMandateId());
    if (!directDebitMandateToVerify.isPresent()) {
      errors.add(DirectDebitMandateUpdateError
          .missingDirectDebitMandate(singleUpdate.getMandateId()));
    } else if (!directDebitMandateToVerify.get().getAccountId().equals(accountId)) {
      errors.add(DirectDebitMandateUpdateError
          .missingDirectDebitMandateForAccount(singleUpdate.getMandateId()));
    }
    return errors;
  }
}
