package uk.gov.caz.whitelist.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import uk.gov.caz.whitelist.annotation.IntegrationTest;

@IntegrationTest
public class WhitelistDataCleanupServiceTestIT {
  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "caz_whitelist_vehicles_audit.logged_actions";
  private int originalNumberOfRecordsInAuditTable;

  @Autowired
  private WhitelistDataCleanupService service;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @BeforeEach
  private void setup() {
    originalNumberOfRecordsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
  }
  
  @Test
  public void shouldCleanupOldData() {
    //when
    whenWeInsertSomeSampleAuditData(18);
    //then
    checkIfAuditTableContainsNumberOfRows(originalNumberOfRecordsInAuditTable + 1);

    //when
    service.cleanupData();
    //then
    checkIfAuditTableContainsNumberOfRows(originalNumberOfRecordsInAuditTable);
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE + " table",
            expectedNumberOfRowsInAuditTable)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }

  private void whenWeInsertSomeSampleAuditData(int age) {
    String sql = String.format("INSERT INTO %s (vrn,schema_name,table_name,user_name,action,"
        + "original_data,new_data,query,insert_timestamp,modifier_id,modifier_email) "
        + "VALUES ('CAS310','TG_TABLE_SCHEMA','TG_TABLE_NAME','session_user','I','{}','{}',"
        + "'current_query', current_timestamp - '%d MONTH'::interval,'modifier_id',"
        + "'modifier_email')",AUDIT_LOGGED_ACTIONS_TABLE,age);
    jdbcTemplate.update(sql);
  }
}