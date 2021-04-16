package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/add-audit-log-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class AuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional
  @Commit
  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoTestTable("Initial Name", "AB123CD");

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(1);
    andAllRowsArePopulatedWithModifierId(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewData("Initial Name", "AB123CD");

    // UPDATE case
    whenWeUpdateTestTableTo("New Name", "Initial Name", "XZ567AB");

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(2);
    andAllRowsArePopulatedWithModifierId(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewData("New Name", "XZ567AB");

    // DELETE case
    whenWeDeleteRowFromTestTable("New Name");

    thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(3);
    andAllRowsArePopulatedWithModifierId(3);
    andThereShouldBeExactlyOneDeleteActionLogged();
    withNewDataEqualToNull();
  }
  
  private void andAllRowsArePopulatedWithModifierId(int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "modifier_id is not null");
  }


  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void whenWeInsertSomeSampleDataIntoTestTable(String name, String vrn) {
    jdbcTemplate.update("INSERT INTO audit.transaction_to_modifier(modifier_id) VALUES (?)",
        UUID.randomUUID().toString());
    jdbcTemplate.update(
        "INSERT INTO public.table_for_audit_test (name, vrn) VALUES (?, ?)", name, vrn);
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableForTestTableShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 'table_for_audit_test'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewData(String name, String vrm) {
    checkIfAuditTableContainsNumberOfRows(1, "'" + toJson(name, vrm) + "'::jsonb @> new_data");
  }

  @SneakyThrows
  private String toJson(String name, String vrm) {
    return objectMapper.writeValueAsString(ImmutableMap.of("vrn", vrm, "name", name));
  }

  private void whenWeUpdateTestTableTo(String newName, String oldName, String newVrn) {
    jdbcTemplate
        .update("UPDATE public.table_for_audit_test " + "set name = ?, vrn = ? " + "where name = ?",
            newName, newVrn, oldName
        );
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromTestTable(String name) {
    jdbcTemplate.update("DELETE from public.table_for_audit_test "
        + "where name = ?", name);
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

