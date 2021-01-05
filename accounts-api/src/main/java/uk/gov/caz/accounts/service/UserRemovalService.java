package uk.gov.caz.accounts.service;

import com.google.common.collect.Lists;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;

/**
 * Service responsible for removing users. A user is permanently deleted from the third-party
 * service, but kept in the database with {@code USER_ID} set to null.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserRemovalService {

  private final IdentityProvider identityProvider;
  private final UserRepository userRepository;
  private final AccountUserCodeRepository accountUserCodeRepository;

  /**
   * Deletes user from the identity provider but does not allow to remove owner users.
   *
   * @param accountId Identifier of the user's account.
   * @param accountUserId User's identifier (internal)
   */
  @Transactional
  public UserRemovalStatus removeStandardUser(UUID accountId, UUID accountUserId) {
    return removeUser(accountId, accountUserId, false);
  }

  /**
   * Deletes user from the identity provider no matter whether the user is owner or standard user.
   *
   * @param accountId Identifier of the user's account.
   * @param accountUserId User's identifier (internal)
   */
  @Transactional
  public UserRemovalStatus removeAnyUser(UUID accountId, UUID accountUserId) {
    return removeUser(accountId, accountUserId, true);
  }

  /**
   * Deletes user from identity provider and marks it as deleted in DB by settings its {@code
   * user_id} to null.
   *
   * @param accountId Identifier of the user's account.
   * @param accountUserId User's identifier (internal)
   */
  private UserRemovalStatus removeUser(UUID accountId, UUID accountUserId, boolean removeOwners) {
    log.info("Trying to remove user with id '{}'", accountUserId);
    UserRemovalValidationResult validationResult = validateRequest(accountId, accountUserId,
        removeOwners);
    if (validationResult.isFailure()) {
      log.info("Failed to remove user with id '{}', validation failed: '{}'", accountUserId,
          validationResult.getUserRemovalStatus());
      return validationResult.getUserRemovalStatus();
    }

    UserEntity user = validationResult.getUser();

    identityProvider.deleteUser(
        identityProvider.getEmailByIdentityProviderId(user.getIdentityProviderUserId())
    );

    discardActiveTokensForUser(user);

    // as the entity state is now managed by the framework we don't need to invoke any custom
    // method on `UserRepository` - the state in database will be updated implicitly
    user.setIdentityProviderUserId(null);

    log.info("Successfully removed user with id '{}'", accountUserId);
    return UserRemovalStatus.SUCCESSFULLY_DELETED;
  }

  private void discardActiveTokensForUser(UserEntity user) {
    accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeTypeIn(
        user.getId(),
        CodeStatus.ACTIVE,
        Lists.newArrayList(CodeType.USER_VERIFICATION, CodeType.PASSWORD_RESET)
    ).forEach(code -> {
      accountUserCodeRepository.save(code.toBuilder().status(CodeStatus.DISCARDED).build());
    });
  }

  private UserRemovalValidationResult validateRequest(UUID accountId, UUID userAccountId,
      boolean removeOwners) {
    Optional<UserEntity> userOptional = userRepository.findById(userAccountId);
    if (!userOptional.isPresent()) {
      return UserRemovalValidationResult.failure(UserRemovalStatus.USER_DOESNT_EXIST);
    }

    UserEntity user = userOptional.get();

    if (!Objects.equals(user.getAccountId(), accountId)) {
      return UserRemovalValidationResult.failure(UserRemovalStatus.USER_DOESNT_EXIST);
    }

    if (user.isOwner() && !removeOwners) {
      return UserRemovalValidationResult.failure(UserRemovalStatus.USER_IS_AN_OWNER);
    }

    if (user.isRemoved()) {
      return UserRemovalValidationResult.failure(UserRemovalStatus.ALREADY_DELETED);
    }

    return UserRemovalValidationResult.success(user);
  }

  @Value
  private static class UserRemovalValidationResult {

    // invariant:
    // if userRemovalStatus is not null, then user is null and vice versa:
    // if user is not null then userRemovalStatus is null
    UserRemovalStatus userRemovalStatus;
    UserEntity user;

    public static UserRemovalValidationResult failure(UserRemovalStatus userRemovalStatus) {
      return new UserRemovalValidationResult(userRemovalStatus, null);
    }

    public static UserRemovalValidationResult success(UserEntity user) {
      return new UserRemovalValidationResult(null, user);
    }

    public boolean isFailure() {
      return userRemovalStatus != null;
    }
  }
}
