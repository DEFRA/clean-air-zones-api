package uk.gov.caz.whitelist.repository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * A class that provides operations on audit log infrastructure.
 */
@Repository
@RequiredArgsConstructor
public class AuditingRepository {

  private final JdbcTemplate jdbcTemplate;

  public void tagModificationsInCurrentTransactionBy(UUID modifierId, String modifierEmail) {
    tagModificationsInCurrentTransactionBy(modifierId.toString(), modifierEmail);
  }

  /**
   * Populates auditing table with modifier ID that will then by 
   * pulled into the auditing actions table. 
   * @param modifierId - CognitoID of user who initialised upload.
   */
  public void tagModificationsInCurrentTransactionBy(String modifierId, String modifierEmail) {
    jdbcTemplate.update(
        "INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier"
            + "(modifier_id, modifier_email) VALUES (?, ?)",
        modifierId, modifierEmail);
  }
}
 