package uk.gov.caz.accounts.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountClosureReason;

/**
 * A class which handles managing data in {@code T_ACCOUNT} table.
 */
@Repository
public interface AccountRepository extends CrudRepository<Account, UUID> {

  List<Account> findAllByNameIgnoreCase(String accountName);

  List<Account> findAllByInactivationTimestampIsNull();

  @Modifying
  @Query("update Account act set act.name = :accountName where act.id = :accountId")
  void updateName(@Param("accountId") UUID accountId, @Param("accountName") String accountName);

  @Modifying
  @Query("update Account act set act.closureReason = :closureReason where act.id = :accountId")
  void updateClosureReason(@Param("accountId") UUID accountId,
      @Param("closureReason") AccountClosureReason closureReason);

  @Modifying
  @Query("update Account act "
      + "set act.multiPayerAccount = :multiPayerAccount "
      + "where act.id = :accountId")
  void updateMultiPayerAccount(@Param("accountId") UUID accountId,
      @Param("multiPayerAccount") Boolean multiPayerAccount);
}
