package uk.gov.caz.accounts.repository;

import static com.google.common.collect.ImmutableList.of;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;

/**
 * Database repository that manages {@link AccountUserCode}s.
 */
@Repository
public interface AccountUserCodeRepository extends CrudRepository<AccountUserCode, Integer> {

  Optional<AccountUserCode> findByCodeAndCodeType(String code, CodeType codeType);

  @Modifying
  @Query("update AccountUserCode account_user_code set account_user_code.status = :status "
      + "where account_user_code.code = :code")
  int setStatusForCode(@Param("code") String code, @Param("status") CodeStatus status);

  default List<AccountUserCode> findByAccountUserIdAndStatusAndCodeType(
      UUID accountUserId, CodeStatus status, CodeType codeType
  ) {
    return findByAccountUserIdAndStatusAndCodeTypeIn(accountUserId, status, of(codeType));
  }

  List<AccountUserCode> findByAccountUserIdAndStatusAndCodeTypeIn(UUID accountUserId,
      CodeStatus status, List<CodeType> codeTypes);

  @Query(value = "SELECT auc.* FROM caz_account.t_account_user_code auc "
      + "LEFT JOIN caz_account.t_account_user au ON au.account_user_id = auc.account_user_id "
      + "LEFT JOIN caz_account.t_account ac ON ac.account_id = au.account_id "
      + "WHERE LOWER(ac.account_name) = LOWER(:accountName) "
      + "AND au.user_id is not null",
      nativeQuery = true)
  List<AccountUserCode> findAllByAccountNameForExistingUsers(
      @Param("accountName") String accountName);

  @Modifying
  @Query("DELETE from AccountUserCode auc where auc.accountUserId = :accountUserId")
  int deleteByAccountUserId(@Param("accountUserId") UUID accountUserId);

  @Query(value =
      "SELECT account_user_code_id, account_user_id, code, expiration, code_type, status "
          + "FROM caz_account.t_account_user_code "
          + "WHERE insert_timestmp >= (CURRENT_TIMESTAMP - INTERVAL '1 hour') "
          + "AND account_user_id = :accountUserId "
          + "AND code_type = :codeType "
          + "ORDER BY account_user_code_id DESC "
          + "LIMIT :limit",
      nativeQuery = true)
  List<AccountUserCode> findByAccountUserIdFromLastHourWithLimit(
      @Param("accountUserId") UUID accountUserId,
      @Param("codeType") String codeType,
      @Param("limit") int limit);
}
