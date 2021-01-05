package uk.gov.caz.accounts.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.repository.exception.NotUniquePaymentProviderMandateIdException;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateDoesNotBelongsToAccountException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateNotFoundException;

@Slf4j
@Service
@AllArgsConstructor
public class DirectDebitMandateService {

  private final DirectDebitMandateRepository directDebitMandateRepository;
  private final AccountRepository accountRepository;

  /**
   * Persists the provided data in the database.
   *
   * @param accountId an ID of the account.
   * @param request provides information about direct debit mandate to store.
   * @return persisted {@link DirectDebitMandate}.
   */
  @Transactional
  public DirectDebitMandate create(UUID accountId, DirectDebitMandateRequest request) {
    verifyAccountPresence(accountId);
    verifyMandateIdIsUnique(request.getMandateId());
    DirectDebitMandate directDebitMandate = buildDirectDebitMandate(accountId, request);
    return directDebitMandateRepository.save(directDebitMandate);
  }

  /**
   * Get all {@link DirectDebitMandate} assigned to the account.
   *
   * @param accountId an ID of the account.
   * @return list of {@link DirectDebitMandate}.
   */
  public List<DirectDebitMandate> findAllByAccountId(UUID accountId) {
    return directDebitMandateRepository.findAllByAccountId(accountId);
  }

  /**
   * Persists the provided data in the database.
   *
   * @param accountId an ID of the account.
   * @param directDebitMandateId an ID of the DirectDebitMandate.
   */
  public void delete(UUID accountId, UUID directDebitMandateId) {
    verifyAccountPresence(accountId);
    DirectDebitMandate directDebitMandate = directDebitMandateRepository
        .findById(directDebitMandateId).orElseThrow(
            () -> new DirectDebitMandateNotFoundException("Direct Debit Mandate not found."));
    verifyDirectDebitMandateBelongsToAccount(directDebitMandate, accountId);
    directDebitMandateRepository.delete(directDebitMandate);
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

  /**
   * Verifies if Direct Debit Mandate belongs to Account.
   *
   * @param directDebitMandate object of DirectDebitMandate
   * @param accountId expected ID of the account.
   */
  private void verifyDirectDebitMandateBelongsToAccount(DirectDebitMandate directDebitMandate,
      UUID accountId) {
    if (!directDebitMandate.getAccountId().equals(accountId)) {
      log.debug("Direct Debit Mandate does not belongs to provided account.");
      throw new DirectDebitMandateDoesNotBelongsToAccountException(
          "DirectDebitMandate does not belongs to provided Account");
    }
  }

  /**
   * Checks that the mandate is not a duplicate.
   *
   * @param mandateId the mandate identifier
   */
  private void verifyMandateIdIsUnique(String mandateId) {
    Optional<DirectDebitMandate> mandate = directDebitMandateRepository
        .findByPaymentProviderMandateId(mandateId);
    if (mandate.isPresent()) {
      log.debug("Direct debit mandate with id " + mandateId + " already exists.");
      throw new NotUniquePaymentProviderMandateIdException(
          "Direct debit mandate with id " + mandateId + " already exists.");
    }
  }

  /**
   * Creates a model object for {@link DirectDebitMandate}.
   *
   * @param accountId an ID of the account.
   * @param request provides information about direct debit mandate to store.
   * @return {@link DirectDebitMandate} object without an ID.
   */
  private DirectDebitMandate buildDirectDebitMandate(UUID accountId,
      DirectDebitMandateRequest request) {
    return DirectDebitMandate.builder()
        .accountId(accountId)
        .paymentProviderMandateId(request.getMandateId())
        .cleanAirZoneId(request.getCleanAirZoneId())
        .build();
  }
}