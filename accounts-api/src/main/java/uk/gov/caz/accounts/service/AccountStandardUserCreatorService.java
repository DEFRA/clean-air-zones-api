package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.InvitingUserNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountStandardUserCreatorService {

  private final UserService userService;
  private final AccountRepository accountRepository;
  private final AccountUserRepository accountUserRepository;
  private final PasswordResetService passwordResetService;
  private final UserPermissionsService userPermissionsService;

  /**
   * Creates a standard user (not owner) for already existing account.
   */
  @Transactional
  public void createStandardUserForAccount(UUID accountId, UUID invitingUserId,
      String email, String name, Set<Permission> permissions, URI verificationUri) {
    final Account account =
        checkPreconditions(accountId, invitingUserId, email, verificationUri);

    log.info("Attempt to create a user invited by user '{}'", invitingUserId);

    // TODO verify permissions to manage users

    User userToBeCreated = buildUser(accountId, invitingUserId, email, name);
    User createdUser = userService.createStandardUser(userToBeCreated);
    userPermissionsService.updatePermissions(accountId, createdUser.getId(), permissions);
    updateAccountProperties(account, permissions);

    log.info("Created user with identityProviderUserId '{}' invited by user '{}'",
        createdUser.getIdentityProviderUserId(), invitingUserId);

    passwordResetService.generateAndSaveResetTokenForInvitedUser(createdUser, account,
        verificationUri);
  }

  private void updateAccountProperties(Account account, Set<Permission> permissions) {
    if (permissions.contains(Permission.MAKE_PAYMENTS)) {
      account.setMultiPayerAccount(Boolean.TRUE);
    }
  }

  /**
   * Creates an instance of {@link User} representing a standard user based on the passed
   * attributes.
   */
  private User buildUser(UUID accountId, UUID invitingUserId, String email, String name) {
    return User.builder()
        .administeredBy(invitingUserId)
        .identityProviderUserId(UUID.randomUUID())
        .email(email)
        .emailVerified(false)
        .name(name)
        .accountId(accountId)
        .isOwner(false)
        .build();
  }

  /**
   * Verifies that the inviting user exists. {@link InvitingUserNotFoundException} is thrown
   * when the user is absent in the database.
   */
  private void verifyPresenceOfInvitingUser(UUID invitingUserId) {
    Optional<User> invitedBy = accountUserRepository.findById(invitingUserId);

    if (!invitedBy.isPresent()) {
      throw new InvitingUserNotFoundException("The user who initiated the invitation was not "
          + "found");
    }
  }

  /**
   * Verifies that the given account exists. {@link AccountNotFoundException} is thrown
   * when the account is absent in the database, otherwise the found {@link Account} is returned.
   */
  private Account verifyPresenceOfMatchingAccount(UUID accountId) {
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException("Account was not found."));
  }

  /**
   * Method performs initial validation for provided parameters.
   */
  private Account checkPreconditions(UUID accountId, UUID invitingUserId, String email,
      URI verificationUri) {
    Preconditions.checkNotNull(accountId, "accountId cannot be null");
    Preconditions.checkNotNull(email, "email cannot be null");
    Preconditions.checkNotNull(verificationUri, "verificationUri cannot be null");
    Account account = verifyPresenceOfMatchingAccount(accountId);
    verifyPresenceOfInvitingUser(invitingUserId);
    return account;
  }
}
