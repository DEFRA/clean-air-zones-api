package uk.gov.caz.accounts.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.dto.DirectDebitMandateUpdateError;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateUpdateException;

/**
 * Service responsible for updating the collection of direct debit mandates.
 */
@Slf4j
@Service
@AllArgsConstructor
public class DirectDebitMandatesBulkUpdater {

  private final AccountRepository accountRepository;
  private final DirectDebitMandateRepository directDebitMandateRepository;
  private final DirectDebitMandateUpdateErrorsCollector directDebitMandateUpdateErrorsCollector;

  /**
   * Updates statuses for the provided mandates.
   *
   * @param accountId an ID of the account.
   * @param singleDirectDebitMandateUpdates provides information about direct debit mandates to
   *     update.
   */
  @Transactional
  public void updateStatuses(UUID accountId,
      List<SingleDirectDebitMandateUpdate> singleDirectDebitMandateUpdates) {
    validateInput(accountId, singleDirectDebitMandateUpdates);
    updateValidMandates(singleDirectDebitMandateUpdates);
  }

  /**
   * Validates the provided input data, throws {@link DirectDebitMandateUpdateException} upon
   * failure.
   */
  private void validateInput(UUID accountId,
      List<SingleDirectDebitMandateUpdate> singleDirectDebitMandateUpdates) {
    verifyAccountPresence(accountId);
    List<DirectDebitMandateUpdateError> errorsList = directDebitMandateUpdateErrorsCollector
        .collectErrors(accountId, singleDirectDebitMandateUpdates);

    if (!errorsList.isEmpty()) {
      throw new DirectDebitMandateUpdateException(errorsList);
    }
  }

  /**
   * Method iterates over the validated updates and updates direct debit mandates accordingly.
   *
   * @param singleDirectDebitMandateUpdates list of updates for the direct debit mandates.
   */
  private void updateValidMandates(
      List<SingleDirectDebitMandateUpdate> singleDirectDebitMandateUpdates) {
    List<DirectDebitMandate> updatedDirectDebitMandatesToSave = singleDirectDebitMandateUpdates
        .stream()
        .map(this::getAndEditDirectDebitMandate)
        .collect(Collectors.toList());

    directDebitMandateRepository.saveAll(updatedDirectDebitMandatesToSave);
  }

  /**
   * Method fetches direct debit mandate from the database and updates the 'status' attribute
   * without saving it back to the database.
   *
   * @param singleUpdate data to update the mandate.
   * @return {@link DirectDebitMandate} with updated attribute.
   */
  private DirectDebitMandate getAndEditDirectDebitMandate(
      SingleDirectDebitMandateUpdate singleUpdate) {
    DirectDebitMandate directDebitMandateToUpdate = directDebitMandateRepository
        .findByPaymentProviderMandateId(singleUpdate.getMandateId())
        .orElseThrow(() -> new IllegalStateException("Mandate does not exist"));

    return directDebitMandateToUpdate.toBuilder()
        .status(DirectDebitMandateStatus.valueOf(singleUpdate.getStatus()))
        .build();
  }

  /**
   * Verifies if there exists an account with provided accountId.
   *
   * @param accountId expected ID of the account.
   */
  private void verifyAccountPresence(UUID accountId) {
    Optional<Account> matchedAccount = accountRepository.findById(accountId);
    if (!matchedAccount.isPresent()) {
      log.debug("Matching account was not found.");
      throw new AccountNotFoundException("Account was not found.");
    }
  }
}
