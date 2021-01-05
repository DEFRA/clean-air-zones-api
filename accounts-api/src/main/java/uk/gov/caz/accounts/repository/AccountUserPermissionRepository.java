package uk.gov.caz.accounts.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.AccountUserPermission;
import uk.gov.caz.accounts.model.AccountUserPermissionId;

/**
 * A class which handles managing data in {@code ACCOUNT_USER_PERMISSION} table.
 */
@Repository
public interface AccountUserPermissionRepository extends
    CrudRepository<AccountUserPermission, AccountUserPermissionId> {

  @Modifying
  @Query("delete from AccountUserPermission aup where aup.accountUserId = :accountUserId")
  void deleteByAccountUserId(@Param("accountUserId") UUID accountUserId);
}
