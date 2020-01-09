package uk.gov.caz.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.tariff.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/add-audit-log-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-audit-log-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class AuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoTestTable('A', "Class A");

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewData("A", "Class A");

    // UPDATE case
    whenWeUpdateCazClassTableTo("Classs A");

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewData("A", "Classs A");

    // DELETE case
    whenWeDeleteRowFromCazClassTable();

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(3);
    andThereShouldBeExactlyOneDeleteActionLogged();
    withNewDataEqualToNull();
  }

  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void whenWeInsertSomeSampleDataIntoTestTable(char cazClass,
      String cazClassDesc) {
    jdbcTemplate
        .update("INSERT INTO public.table_for_audit_test (caz_class, caz_class_desc) VALUES (?, ?)",
            cazClass, cazClassDesc);
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 'table_for_audit_test'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewData(String cazClass, String cazClassDesc) {
    checkIfAuditTableContainsNumberOfRows(1,
        "'" + toJson(cazClass, cazClassDesc) + "'::jsonb @> new_data");
  }

  private void whenWeUpdateCazClassTableTo(String cazClassDesc) {
    jdbcTemplate.update(
        "UPDATE public.table_for_audit_test set caz_class_desc = ?",
        cazClassDesc);
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromCazClassTable() {
    jdbcTemplate.update("DELETE from public.table_for_audit_test");
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

  @SneakyThrows
  private String toJson(String cazClass, String cazClassDesc) {
    return objectMapper
        .writeValueAsString(ImmutableMap.of("caz_class", cazClass, "caz_class_desc", cazClassDesc));
  }
}