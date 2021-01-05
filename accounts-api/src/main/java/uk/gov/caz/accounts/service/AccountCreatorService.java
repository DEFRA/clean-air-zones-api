package uk.gov.caz.accounts.service;

import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.validation.DuplicateAccountValidator;

/**
 * Service responsible for creating Account and Admin User for the created account.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountCreatorService {

  private final AbusiveLanguageValidator abusiveLanguageValidator;
  private final AccountRepository accountRepository;
  private final DuplicateAccountValidator duplicateAccountValidator;

  /**
   * Creates (internally) the account. NOTE: this does not create a user for the account.
   */
  @Transactional
  public Account createAccount(String accountName) {
    checkCreateAccountPreconditions(accountName);
    log.info("Creating account {}", mask(accountName));

    Account account = accountRepository.save(Account.builder()
        .name(StringUtils.normalizeSpace(accountName))
        .build());

    log.info("Created account {} with accountId {}", mask(accountName),
        account.getId());
    return account;
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
