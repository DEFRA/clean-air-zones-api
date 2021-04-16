package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;
import uk.gov.caz.accounts.util.Strings;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final IdentityProvider identityProvider;
  private final AccountUserRepository accountUserRepository;
  private final UserRepository userRepository;
  private final UserPermissionsService userPermissionsService;
  private final RecentlyUsedPasswordChecker recentlyUsedPasswordChecker;

  /**
   * Method that creates a standard user.
   *
   * @param user object containing user details.
   * @return User object which contains User details with created IDs
   * @throws IdentityProviderUnavailableException when cognito call fails.
   */
  @Transactional
  public UserEntity createStandardUser(UserEntity user) {
    Preconditions.checkArgument(!user.isOwner(), "User cannot be an owner");
    Preconditions.checkArgument(Objects.nonNull(user.getIdentityProviderUserId()),
        "User identity provider id must not be null");
    Preconditions.checkArgument(Objects.isNull(user.getId()), "User id must be null");
    requireUniqueEmail(user.getEmail());

    UserEntity createdUser = createInternalUserInDatabase(user);
    return createExternalStandardUser(createdUser);
  }

  /**
   * Method that creates Standard user in cognito service.
   *
   * @param user object containing user details.
   * @return User object which contains User details with created IDs
   * @throws IdentityProviderUnavailableException when cognito call fails.
   */
  private UserEntity createExternalStandardUser(UserEntity user) {
    // assertion: we assume the uniqueness of email has already been checked
    return identityProvider.createStandardUser(user);
  }

  /**
   * Method that creates a standard user.
   *
   * @param email of a new user.
   * @param name of a new user.
   * @param invitingUserId of Admin user who send the invitation.
   * @param accountId ID of associated Account.
   * @return User object which contains User details with created IDs
   * @throws IdentityProviderUnavailableException when cognito call fails.
   */
  @Transactional
  public UserEntity createStandardUserForExistingEmail(String email, String name,
      UUID invitingUserId, UUID accountId) {
    UserEntity existingUser = identityProvider.getUserAsUserEntity(email);

    UserEntity createdInternalUser = createInternalUserInDatabase(existingUser.toBuilder()
        .isOwner(false)
        .name(name)
        .accountId(accountId)
        .isAdministratedBy(invitingUserId)
        .accountPermissions(Collections.emptyList())
        .build()
    );

    return createdInternalUser;
  }

  /**
   * Creates Admin user (also known as fleet manager) and sets their permissions provided this is
   * the first user of the account. Admin user is a person which registers account and manages
   * account, vehicles, payments and users. If there is an error upon account creation a rollback
   * attempt is made to delete the account provided it has been created.
   *
   * @param email of a new user.
   * @param password of a new user.
   * @param accountId ID of associated Account.
   * @return User object which contains User details with created IDs
   * @throws IdentityProviderUnavailableException when cognito call fails.
   */
  @Transactional
  public UserEntity createAdminUser(String email, String password, UUID accountId) {
    requireUniqueEmail(email);

    UserEntity adminUser = createBaseAdminUserEntityForAccount(accountId, email);
    UserEntity adminUserWithIds = createInternalUserInDatabase(adminUser);
    setDefaultPermissionsForUser(adminUserWithIds);
    createExternalAdminUser(adminUserWithIds.getIdentityProviderUserId(), email, password);
    return adminUserWithIds;
  }

  private UserEntity createBaseAdminUserEntityForAccount(UUID accountId, String email) {
    UserEntity adminUser = new UserEntity();
    adminUser.setAccountId(accountId);
    adminUser.setOwner(true);
    adminUser.setIdentityProviderUserId(UUID.randomUUID());
    adminUser.setAccountPermissions(Collections.emptyList());
    adminUser.setEmail(email);
    return adminUser;
  }

  /**
   * Method asks {@link IdentityProvider} for {@link User} and then fetches all user details from
   * the database. If user is not present in {@link IdentityProvider} or in the database returns
   * Optional.empty(). Returns {@link User} with properties combined from our DB and
   * IdentityProvider.
   *
   * @param email of user whose details are going to be fetched.
   * @return User object with all required details.
   */
  public Optional<User> getUserByEmail(String email) {
    if (!identityProvider.checkIfUserExists(email)) {
      return Optional.empty();
    }
    User identityProviderUser = identityProvider.getUser(email);
    Optional<User> dbUser = accountUserRepository
        .findByUserId(identityProviderUser.getIdentityProviderUserId());
    return dbUser.map(enrichWithIdentityProviderProperties(identityProviderUser));
  }

  /**
   * Method asks {@link IdentityProvider} for {@link UserEntity} and then fetches all user details
   * from the database. If user is not present in {@link IdentityProvider} or in the database
   * returns Optional.empty(). Returns {@link UserEntity} with properties combined from our DB and
   * IdentityProvider.
   *
   * @param email of user whose details are going to be fetched.
   * @return User object with all required details.
   */
  public Optional<UserEntity> getUserEntityByEmail(String email) {
    if (!identityProvider.checkIfUserExists(email)) {
      return Optional.empty();
    }
    UserEntity identityProviderUser = identityProvider.getUserAsUserEntity(email);
    return userRepository.findByIdentityProviderUserId(
        identityProviderUser.getIdentityProviderUserId()
    ).map(dbUser -> {
      dbUser.setEmail(identityProviderUser.getEmail());
      dbUser.setEmailVerified(identityProviderUser.isEmailVerified());
      return dbUser;
    });
  }

  /**
   * Method receives {@link User} object and fetches more details about him from the third-party
   * service.
   *
   * @param accountUserId user stored in the database.
   * @return complete set of information about {@link User}
   */
  public User getCompleteUserDetailsForAccountUserId(UUID accountUserId) {
    User dbUser = accountUserRepository.findById(accountUserId)
        .orElseThrow(() -> new AccountUserNotFoundException("AccountUser was not found."));

    String email = identityProvider
        .getEmailByIdentityProviderId(dbUser.getIdentityProviderUserId());

    return getUserByEmail(email).orElseThrow(
        () -> new AccountUserNotFoundException("Account was not found in third party service"));
  }

  /**
   * Method receives {@link User} object and fetches more details about him from the third-party
   * service.
   *
   * @param accountUserId user stored in the database.
   * @return complete set of information about {@link User}
   */
  public UserEntity getCompleteUserDetailsAsUserEntityForAccountUserId(UUID accountUserId) {
    UserEntity dbUser = userRepository.findById(accountUserId)
        .orElseThrow(() -> new AccountUserNotFoundException("AccountUser was not found."));

    String email = identityProvider.getEmailByIdentityProviderId(
        dbUser.getIdentityProviderUserId());

    return getUserEntityByEmail(email).orElseThrow(
        () -> new AccountUserNotFoundException("Account was not found in third party service"));
  }

  /**
   * Fetches user's data from DB and the external store. If the user does not exist, returns {@link
   * Optional#empty()}.
   */
  public Optional<UserEntity> findUser(UUID accountUserId) {
    return userRepository.findById(accountUserId).map(this::enhanceUserWithExternalDetails);
  }

  /**
   * Updates details of the passed {@code user} with attributes that are stored externally (by
   * creating a new instance of {@link UserEntity}), provided the user is not removed. Returns the
   * passed {@code user} otherwise.
   */
  private UserEntity enhanceUserWithExternalDetails(UserEntity user) {
    if (user.isRemoved()) {
      return user;
    }
    UserEntity identityProviderUser = identityProvider.getUserAsUserEntityByExternalId(
        user.getIdentityProviderUserId());
    return user.toBuilder()
        .name(identityProviderUser.getName())
        .identityProviderUserId(identityProviderUser.getIdentityProviderUserId())
        .email(identityProviderUser.getEmail())
        .emailVerified(identityProviderUser.isEmailVerified())
        .build();
  }

  /**
   * Combines user data kept in our DB with additional properties kept in IdentityProvider.
   */
  private Function<User, User> enrichWithIdentityProviderProperties(User identityProviderUser) {
    return dbUser -> User.combinedDbAndIdentityProvider(dbUser, identityProviderUser);
  }

  /**
   * Method that updates users password in Identity provider.
   *
   * @param user object containing user details.
   * @param password new password which need to be set.
   * @throws IdentityProviderUnavailableException when identity provider call fails.
   */
  public void setPassword(User user, String password) {
    setPassword(password, user.getIdentityProviderUserId());
  }

  /**
   * Method that updates users password in Identity provider.
   *
   * @param user object containing user details.
   * @param password new password which need to be set.
   * @throws IdentityProviderUnavailableException when identity provider call fails.
   */
  public void setPassword(UserEntity user, String password) {
    setPassword(password, user.getIdentityProviderUserId());
  }

  private void setPassword(String password, UUID identityProviderId) {
    try {
      String email = identityProvider.getEmailByIdentityProviderId(identityProviderId);
      recentlyUsedPasswordChecker.checkIfPasswordWasNotUsedRecently(password, email);
      identityProvider.setUserPassword(email, password);
    } catch (Exception e) {
      log.error("Error occurs during setting user password, message: '{}'", e.getMessage());
      throw e;
    }
  }

  /**
   * Method that gets user with Identity Provider details. Note that it gets user details from
   * IdentityProvider sequentially for each user.
   *
   * @param accountId ID of associated Account.
   * @throws IdentityProviderUnavailableException when identity provider call fails.
   */
  public List<UserEntity> getAllUsersForAccountId(UUID accountId) {
    log.info("Getting Standard users for '{}'", accountId);
    List<UserEntity> users = userRepository.findAllByAccountId(accountId);
    return users.stream()
        .map(this::getUserDetails)
        .collect(Collectors.toList());
  }

  /**
   * Method that gets user with Identity Provider details.
   *
   * @param accountId ID of associated Account.
   * @throws IdentityProviderUnavailableException when identity provider call fails.
   */
  @Transactional
  public Optional<UserEntity> getUserForAccountId(UUID accountId, UUID accountUserId) {
    log.info("Getting Standard user {}", accountUserId);

    return userRepository.findByIdAndAccountId(accountUserId, accountId)
        .filter(user -> !user.isRemoved())
        .map(identityProvider::getUserDetailsByIdentityProviderId);
  }

  /**
   * Method creates internal user for already existing external user.
   *
   * @param email email of user already existing in external service.
   * @param password new password for existing user.
   * @param accountId account to which user will be assigned.
   * @return created {@link User}.
   */
  @Transactional
  public UserEntity createAdminUserForExistingEmail(String email, String password, UUID accountId) {
    UserEntity existingUser = identityProvider.getUserAsUserEntity(email);
    UserEntity createdInternalUser = createInternalUserInDatabase(existingUser.toBuilder()
        .isOwner(true)
        .accountId(accountId)
        .accountPermissions(Collections.emptyList())
        .build()
    );
    setDefaultPermissionsForUser(createdInternalUser);
    setPassword(createdInternalUser, password);
    return createdInternalUser;
  }

  /**
   * Gets timestamp of when was the last password update (or user creation if not yet known).
   *
   * @param email email {@link String} containing email.
   * @return timestamp of when was the last password update (or user creation if not yet known).
   */
  public LocalDateTime getPasswordUpdateTimestamp(String email) {
    return identityProvider.getPasswordUpdateTimestamp(email);
  }

  /**
   * Sets all permissions after admin user creation.
   *
   * @param createdUser newly created {@link User}.
   */
  private void setDefaultPermissionsForUser(UserEntity createdUser) {
    Set<Permission> allPermissions = EnumSet.allOf(Permission.class);
    userPermissionsService.updatePermissions(createdUser.getAccountId(), createdUser.getId(),
        allPermissions);
  }

  /**
   * Method that creates Admin user in cognito service.
   *
   * @param email of user who wants to create account.
   * @param password of user who wants to create account.
   * @throws IdentityProviderUnavailableException when cognito call fails.
   */
  private void createExternalAdminUser(UUID identityProviderId, String email, String password) {
    // assertion: we assume the uniqueness of email has already been checked
    identityProvider.createAdminUser(identityProviderId, email, password);
  }

  /**
   * Method that creates user in internal database. In case of exception, removes user in cognito
   * service.
   *
   * @param user object containing user details.
   * @return User object which contains User details with created IDs
   */
  private UserEntity createInternalUserInDatabase(UserEntity user) {
    Preconditions.checkNotNull(user.getIdentityProviderUserId(), "user id cannot be null");
    Preconditions.checkNotNull(user.getAccountId(), "account id cannot be null");
    Preconditions.checkArgument(Objects.isNull(user.getId()), "id must be null");

    return userRepository.save(user);
  }

  /**
   * Method that validates email address uniqueness.
   *
   * @param email Email to be checked.
   */
  private void requireUniqueEmail(String email) {
    boolean userExists = identityProvider.checkIfUserExists(email);
    if (userExists) {
      checkDataConsistency(email);
      log.info("User with email {} exists in the DB.", Strings.mask(email));
      throw new NotUniqueEmailException("User with given email already exists.");
    }
  }

  /**
   * Method that validates data consistency between IdentityProvider and local DB.
   *
   * @param email of user to be checked.
   */
  private void checkDataConsistency(String email) {
    User identityProviderUser = identityProvider.getUser(email);
    Optional<User> dbUser = accountUserRepository
        .findByUserId(identityProviderUser.getIdentityProviderUserId());

    if (!dbUser.isPresent()) {
      log.error("Data inconsistency: User with identity-provider-id {} is not stored in DB",
          identityProviderUser.getIdentityProviderUserId());
      throw new IdentityProviderUnavailableException("External Service Failure");
    }
  }

  /**
   * Method that fetches user details using Identity Provider unless the user is already removed.
   *
   * @param user user for which the details are supposed to be fetched
   * @return {@link UserEntity} object.
   * @throws IdentityProviderUnavailableException when identity provider call fails.
   */
  private UserEntity getUserDetails(UserEntity user) {
    if (!user.isRemoved()) {
      return identityProvider.getUserDetailsByIdentityProviderId(user);
    }
    return user;
  }
}
