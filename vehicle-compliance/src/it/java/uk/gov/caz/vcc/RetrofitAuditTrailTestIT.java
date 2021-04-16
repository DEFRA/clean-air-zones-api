package uk.gov.caz.vcc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-retrofit-tables.sql",
    "classpath:data/sql/add-retrofit-audit-log-data.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-retrofit-audit-log-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class RetrofitAuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoTestTable("CAS307");
    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewDataLike("Category1");

    // UPDATE case
    whenWeUpdateRetrofitVehiclesTo("Category1", "Category2");

    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewDataLike("Category2");

    // DELETE case
    whenWeDeleteRowFromWRetrofitVehiclesTable("CAS307");

    thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(3);
    andThereShouldBeExactlyOneDeleteActionLogged();
    withNewDataEqualToNull();
  }

  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void whenWeInsertSomeSampleDataIntoTestTable(String vrn) {
    jdbcTemplate.update(
        "INSERT INTO table_for_retrofit_audit_test(VRN, VEHICLE_CATEGORY, MODEL) "
            + "VALUES (?, ?, ?)", vrn, "Category1", "Model1");
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableWithCAS307ShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 'table_for_retrofit_audit_test'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewDataLike(String category) {
    checkIfAuditTableContainsNumberOfRows(1,
        "new_data ->> 'vrn' = 'CAS307' and new_data ->> 'vehicle_category' = '" + category + "'");
  }

  private void whenWeUpdateRetrofitVehiclesTo(String oldCategory, String newCategory) {
    jdbcTemplate.update("UPDATE table_for_retrofit_audit_test "
            + "set VEHICLE_CATEGORY = ? "
            + "where VEHICLE_CATEGORY = ?",
        newCategory, oldCategory
    );
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromWRetrofitVehiclesTable(String vrn) {
    jdbcTemplate.update("DELETE from table_for_retrofit_audit_test "
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