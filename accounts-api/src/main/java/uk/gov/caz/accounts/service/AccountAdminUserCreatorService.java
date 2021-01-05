package uk.gov.caz.accounts.service;

import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountAdminUserCreatorService {

  private final UserService userService;
  private final AccountRepository accountRepository;
  private final PendingOwnersRemovalService pendingOwnersRemovalService;
  private final TokensExpiryDatesProvider tokensExpiryDatesProvider;
  private final DuplicatedAccountUserService duplicatedAccountUserService;
  private final VerificationEmailIssuerService verificationEmailIssuerService;

  /**
   * Service method that creates a user for already existing account.
   */
  public Pair<UserEntity, Account> createAdminUserForAccount(UUID accountId,
      String email, String password, URI verificationUri) {
    checkPreconditions(accountId, email, password, verificationUri);

    log.info("Creating user '{}' for '{}' account", mask(email), accountId);

    Optional<Account> account = accountRepository.findById(accountId);

    if (!account.isPresent()) {
      log.info("Account for user with email '{}' was not found.", mask(email));
      throw new AccountNotFoundException("Account was not found.");
    }

    UserEntity createdUser = createOrAlterUser(email, password, accountId);

    log.info("Created user with identityProviderUserId '{}'",
        createdUser.getIdentityProviderUserId());

    verificationEmailIssuerService.generateVerificationTokenAndSendVerificationEmail(createdUser,
        verificationUri, generateExpirationDate());

    return Pair.of(createdUser, account.get());
  }

  /**
   * Creates new {@link UserEntity} with provided parameters or removes it along with associated
   * verification tokens.
   *
   * @param email new user email
   * @param password new user password
   * @param accountId accountId with which new user will be associated
   * @return created {@link UserEntity}.
   */
  private UserEntity createOrAlterUser(String email, String password, UUID accountId) {
    if (userIsAlreadyRegistered(email)) {
      duplicatedAccountUserService.resolveAccountUserDuplication(email);
      return userService.createAdminUserForExistingEmail(email, password, accountId);
    }
    pendingOwnersRemovalService.removeNonVerifiedOwnerUsers(accountId);
    return userService.createAdminUser(email, password, accountId);
  }

  /**
   * Calls service to receive the expiration date for the verification.
   *
   * @return {@link LocalDateTime} object which specifies when the link will expire
   */
  private LocalDateTime generateExpirationDate() {
    return tokensExpiryDatesProvider.getVerificationEmailExpiryDateFromNow();
  }

  /**
   * Method checks if user with provided email already exists in the third party identity service.
   *
   * @param email email provided by the user
   */
  private boolean userIsAlreadyRegistered(String email) {
    return userService.getUserEntityByEmail(email).isPresent();
  }

  /**
   * Method performs initial validation for provided parameters.
   */
  private void checkPreconditions(UUID accountId, String email, String password,
      URI verificationUri) {
    Preconditions.checkNotNull(accountId, "accountId cannot be null");
    Preconditions.checkNotNull(email, "email cannot be null");
    Preconditions.checkNotNull(password, "password cannot be null");
    Preconditions.checkNotNull(verificationUri, "verificationUri cannot be null");
  }

}
