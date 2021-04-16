package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.validation.DuplicateAccountValidator;

/**
 * Service responsible for updating Account.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountUpdateService {

  private final AccountRepository accountRepository;
  private final AbusiveLanguageValidator abusiveLanguageValidator;
  private final DuplicateAccountValidator duplicateAccountValidator;

  /**
   * Updates Account name.
   */
  @Transactional
  public void updateAccountName(UUID accountId, String accountName) {
    verifyAccountPresence(accountId);
    checkCreateAccountPreconditions(accountName);
    accountRepository.updateName(accountId, accountName);
  }

  /**
   * Updates Account MultiPayerAccount.
   */
  @Transactional
  public void updateMultiPayerAccount(Account account, Set<Permission> permissions) {
    if (permissions.contains(Permission.MAKE_PAYMENTS)) {
      accountRepository.updateMultiPayerAccount(account.getId(), Boolean.TRUE);
    }
  }


  /**
   * Throws {@link AccountNotFoundException} when account with specified ID does not exist.
   */
  private void verifyAccountPresence(UUID accountId) {
    Optional<Account> account = accountRepository.findById(accountId);
    if (!account.isPresent()) {
      throw new AccountNotFoundException("Account was not found.");
    }
  }

  /**
   * Method verifies if account name can be used.
   *
   * @param accountName provided account name
   */
  private void checkCreateAccountPreconditions(String accountName) {
    Preconditions.checkNotNull(accountName, "'accountName' cannot be null");
    duplicateAccountValidator.validate(accountName);
    abusiveLanguageValidator.validate(accountName);
  }
}
