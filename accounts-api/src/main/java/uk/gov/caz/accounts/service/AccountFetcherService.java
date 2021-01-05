package uk.gov.caz.accounts.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.repository.AccountRepository;

/**
 * Service returning account or information about it.
 */
@Service
@RequiredArgsConstructor
public class AccountFetcherService {

  private final AccountRepository accountRepository;

  /**
   * Finds account based on its ID.
   * @param accountId id of account
   * @return account
   */
  public Optional<Account> findById(UUID accountId) {
    return accountRepository.findById(accountId);
  }
}
