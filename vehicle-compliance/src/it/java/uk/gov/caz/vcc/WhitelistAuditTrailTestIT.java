package uk.gov.caz.vcc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear-whitelist-tables.sql",
    "classpath:data/sql/add-whitelist-audit-log-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-whitelist-audit-log-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class WhitelistAuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions";
  private static final UUID TYPICAL_REGISTER_JOB_UPLOADER_ID = java.util.UUID
      .fromString("11111111-2222-3333-4444-555555555555");

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoTestTable("CAS307");
    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewDataLike("Reason1");

    // UPDATE case
    whenWeUpdateWhitelistVehiclesTo("Reason1", "Reason2");

    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewDataLike("Reason2");

    // DELETE case
    whenWeDeleteRowFromWhitelistVehiclesTable("CAS307");

    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(3);
    andThereShouldBeExactlyOneDeleteActionLogged();
    withNewDataEqualToNull();
  }

  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void whenWeInsertSomeSampleDataIntoTestTable(String vrn) {
    jdbcTemplate.update(
        "INSERT INTO table_for_whitelist_audit_test(VRN, MANUFACTURER, REASON_UPDATED, UPLOADER_ID) "
            + "VALUES (?, ?, ?, ?)", vrn, "Manu1", "Reason1", TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 'table_for_whitelist_audit_test' and VRN = 'CAS307'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewDataLike(String reason) {
    checkIfAuditTableContainsNumberOfRows(1,
        "new_data ->> 'vrn' = 'CAS307' and new_data ->> 'reason_updated' = '" + reason + "'");
  }

  private void whenWeUpdateWhitelistVehiclesTo(String oldReason, String newReason) {
    jdbcTemplate.update("UPDATE table_for_whitelist_audit_test "
            + "set REASON_UPDATED = ? "
            + "where REASON_UPDATED = ?",
        newReason, oldReason
    );
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromWhitelistVehiclesTable(String vrn) {
    jdbcTemplate.update("DELETE from table_for_whitelist_audit_test "
        + "where VRN = ?", vrn);
  }

  private void andThereShouldBeExactlyOneDeleteActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'D'");
  }

  private void withNewDataEqualToNull() {
    checkIfAuditTableContainsNumberOfRows(1, "new_data is null");
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE + " table",
            expectedNumberOfRowsInAuditTable)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable,
      String whereClause) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE, whereClause);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE
                + " table matching where clause '%s'",
            expectedNumberOfRowsInAuditTable, whereClause)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }
}