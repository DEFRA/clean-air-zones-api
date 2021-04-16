package uk.gov.caz.accounts.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * A class which handles managing data in {@code ACCOUNT_USER} table.
 */
@Repository
public interface UserRepository extends CrudRepository<UserEntity, UUID> {

  Optional<UserEntity> findByIdAndAccountId(UUID userId, UUID accountId);

  Optional<UserEntity> findByIdentityProviderUserId(UUID identityProviderUserId);

  int countByAccountId(UUID accountId);

  List<UserEntity> findAllByAccountId(UUID accountId);

  List<UserEntity> findAllByAccountIdAndIdentityProviderUserIdIsNotNull(UUID accountId);

  default List<UserEntity> findAllActiveUsersByAccountId(UUID accountId) {
    return findAllByAccountIdAndIdentityProviderUserIdIsNotNull(accountId);
  }

  @Query(value = "SELECT MAX(last_sign_in_timestmp) "
      + "FROM caz_account.t_account_user WHERE "
      + "account_id = :accountId",
      nativeQuery = true)
  Optional<Timestamp> getLatestUserSignInForAccount(UUID accountId);

  List<UserEntity> findByAccountIdAndIsOwner(UUID accountId, boolean isOwner);

  default List<UserEntity> findOwnersForAccount(UUID accountId) {
    return findByAccountIdAndIsOwner(accountId, true);
  }

  @Modifying
  @Query(value = "UPDATE caz_account.t_account_user "
      + "SET LAST_SIGN_IN_TIMESTMP = CURRENT_TIMESTAMP "
      + "WHERE account_user_id = :userId",
      nativeQuery = true)
  void setLastSingInTimestamp(UUID userId);

  @Modifying
  @Query(value = "UPDATE caz_account.t_account_user "
      + "SET pending_user_id = NULL "
      + "WHERE account_user_id = :userId",
      nativeQuery = true)
  void clearPendingUserId(UUID userId);
}
