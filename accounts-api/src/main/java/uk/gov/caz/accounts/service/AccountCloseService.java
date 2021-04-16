package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountClosureReason;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.emailnotifications.AccountCloseEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.EmailContext;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

/**
 * Service responsible for account inactivation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountCloseService {

  private final AccountRepository accountRepository;
  private final IdentityProvider identityProvider;
  private final AccountCloseEmailSender accountCloseEmailSender;
  private final InactivateAccountService inactivateAccountService;
  private final UserRepository userRepository;

  /**
   * Update closure reason field, close account and send email notification.
   *
   * @param accountId id of account
   * @param closureReason the reason of closing an account
   */
  @Transactional
  public void closeAccount(UUID accountId, String closureReason) {
    verifyClosureReasonPresence(closureReason);
    Account account = verifyAccountPresence(accountId);
    getAllActiveUsersEmailsByAccountId(accountId).forEach((email) -> accountCloseEmailSender
        .send(email, EmailContext.of(account)));
    inactivateAccountService.inactivateAccount(accountId);
    accountRepository.updateClosureReason(accountId, AccountClosureReason.valueOf(closureReason));
  }

  /**
   * Verifies if there exists an account with provided accountId.
   *
   * @param accountId expected ID of the account.
   * @throws AccountNotFoundException when account with specified ID does not exist.
   */
  private Account verifyAccountPresence(UUID accountId) {
    return accountRepository.findById(accountId).orElseThrow(
        () -> new AccountNotFoundException("Account was not found."));
  }

  /**
   * Method verifies if closure reason is not null.
   *
   * @param closureReason provided closure reason of account
   */
  private void verifyClosureReasonPresence(String closureReason) {
    Preconditions.checkNotNull(closureReason, "'closureReason' cannot be null");
  }

  /**
   * Method fetches all users of the provided account.
   *
   * @param accountId accountId which owners are going to be fetched.
   * @return list of users emails.
   */
  private List<String> getAllActiveUsersEmailsByAccountId(UUID accountId) {
    List<UserEntity> allUsers = userRepository.findAllActiveUsersByAccountId(accountId);

    return allUsers
        .stream()
        .map((owner) -> getExistingUserEmail(owner))
        .filter((email) -> email != null)
        .collect(Collectors.toList());
  }

  /**
   * Method used solely for the situation on the DEV environment when local DB state can differ from
   * the IdentityProvider state. When a user is present in the local DB and it cannot be found in
   * the IdentityProvider, instead of raising an exception we skip the non-existing email.
   */
  private String getExistingUserEmail(UserEntity user) {
    String email = null;

    try {
      email = identityProvider
          .getEmailByIdentityProviderId(user.getIdentityProviderUserId());
    } catch (IdentityProviderUnavailableException exception) {
      log.error("Locally stored user was not found in the IdentityProvider");
    }

    return email;
  }
}
