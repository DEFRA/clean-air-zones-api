package uk.gov.caz.accounts.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;

/**
 * A class which handles managing data in {@code ACCOUNT_REPOSITORY} table.
 */
@Repository
public interface AccountPermissionRepository extends CrudRepository<AccountPermission, Integer> {

  @Modifying
  @Query(value = "DELETE from CAZ_ACCOUNT.T_ACCOUNT_USER_PERMISSION "
      + "WHERE account_user_id = :accountUserId",
      nativeQuery = true)
  int deleteByAccountUserId(@Param("accountUserId") UUID accountUserId);

  Optional<AccountPermission> findByName(Permission name);
}
