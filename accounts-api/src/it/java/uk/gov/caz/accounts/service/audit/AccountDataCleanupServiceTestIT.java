package uk.gov.caz.accounts.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;

import uk.gov.caz.accounts.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/add-audit-log-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-audit-log-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class AccountDataCleanupServiceTestIT {
  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "caz_account_audit.t_logged_actions";

  @Autowired
  private AccountDataCleanupService service;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @Test
  public void shouldCleanupOldData() {
    //given
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();
    
    //when
    whenWeInsertSomeSampleAuditData();
    //then
    checkIfAuditTableContainsNumberOfRows(1);

    //when
    service.cleanupData();
    //then
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE + " table",
            expectedNumberOfRowsInAuditTable)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }

  private void whenWeInsertSomeSampleAuditData() {
    String sql = "INSERT INTO caz_account_audit.t_logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query,action_tstamp) "
        + "VALUES ('TG_TABLE_SCHEMA','TG_TABLE_NAME','session_user','I','{}','{}', 'current_query', current_timestamp - '12 MONTH'::interval)";
    jdbcTemplate.update(sql);
  }
}