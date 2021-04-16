package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.InvitingUserNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountStandardUserCreatorService {

  private final AccountRepository accountRepository;
  private final AccountUpdateService accountUpdateService;
  private final DuplicatedAccountUserService duplicatedAccountUserService;
  private final IdentityProvider identityProvider;
  private final PasswordResetService passwordResetService;
  private final UserPermissionsService userPermissionsService;
  private final UserRepository userRepository;
  private final UserService userService;

  /**
   * Creates a standard user (not owner) for already existing account.
   */
  public void createStandardUserForAccount(UUID accountId, UUID invitingUserId,
      String email, String name, Set<Permission> permissions, URI verificationUri) {
    final Account account =
        checkPreconditions(accountId, invitingUserId, email, verificationUri);

    log.info("Attempt to create a user invited by user '{}'", invitingUserId);

    // TODO verify permissions to manage users

    UserEntity createdUser = createOrAlterUser(accountId, invitingUserId, email, name);

    userPermissionsService.updatePermissions(accountId, createdUser.getId(), permissions);
    accountUpdateService.updateMultiPayerAccount(account, permissions);

    log.info("Created user with identityProviderUserId '{}' invited by user '{}'",
        createdUser.getIdentityProviderUserId(), invitingUserId);

    passwordResetService.generateAndSaveResetTokenForInvitedUser(createdUser, account,
        verificationUri);
  }

  /**
   * Creates new {@link UserEntity} with provided parameters or removes it along with associated
   * verification tokens.
   *
   * @param accountId accountId with which new user will be associated
   * @param invitingUserId of Admin user who creates Standard User
   * @param email new user email
   * @param name new suer name
   * @return created {@link UserEntity}.
   */
  private UserEntity createOrAlterUser(UUID accountId, UUID invitingUserId, String email,
      String name) {
    if (userIsAlreadyRegistered(email)) {
      duplicatedAccountUserService.resolveAccountUserDuplication(email);
      identityProvider.setUserName(email, name);
      return userService.createStandardUserForExistingEmail(email, name, invitingUserId, accountId);
    }
    UserEntity userToBeCreated = buildUser(accountId, invitingUserId, email, name);
    return userService.createStandardUser(userToBeCreated);
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
   * Creates an instance of {@link User} representing a standard user based on the passed
   * attributes.
   */
  private UserEntity buildUser(UUID accountId, UUID invitingUserId, String email, String name) {
    return UserEntity.builder()
        .isAdministratedBy(invitingUserId)
        .identityProviderUserId(UUID.randomUUID())
        .email(email)
        .emailVerified(false)
        .name(name)
        .accountId(accountId)
        .isOwner(false)
        .build();
  }

  /**
   * Verifies that the inviting user exists. {@link InvitingUserNotFoundException} is thrown when
   * the user is absent in the database.
   */
  private void verifyPresenceOfInvitingUser(UUID invitingUserId) {
    Optional<UserEntity> invitedBy = userRepository.findById(invitingUserId);

    if (!invitedBy.isPresent()) {
      throw new InvitingUserNotFoundException("The user who initiated the invitation was not "
          + "found");
    }
  }

  /**
   * Verifies that the given account exists. {@link AccountNotFoundException} is thrown when the
   * account is absent in the database, otherwise the found {@link Account} is returned.
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
