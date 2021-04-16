package uk.gov.caz.vcc.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;

import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear-vehicle-entrant-audit-log-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class VehicleEntrantDataCleanupServiceTestIT {
  @Autowired
  private VehicleEntrantDataCleanupService service;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @Test
  public void shouldCleanupOldData() {
    //given
    auditTablesShouldBeEmpty();

    //when
    whenWeInsertOneSampleAuditData();
    //then
    checkIfTableContainsNumberOfRows(1,"caz_vehicle_entrant.t_failed_identification_logs");
    checkIfTableContainsNumberOfRows(2,"caz_vehicle_entrant_audit.logged_actions");

    //when
    service.cleanupData();
    //then
    checkIfTableContainsNumberOfRows(0,"caz_vehicle_entrant.t_failed_identification_logs");
    checkIfTableContainsNumberOfRows(2,"caz_vehicle_entrant_audit.logged_actions");
  }

  private void auditTablesShouldBeEmpty() {
    checkIfTableContainsNumberOfRows(0,"caz_vehicle_entrant.t_failed_identification_logs");
    checkIfTableContainsNumberOfRows(0,"caz_vehicle_entrant_audit.logged_actions");
  }

  private void checkIfTableContainsNumberOfRows(int expectedNumberOfRows, String tableName) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + tableName + " table",
            expectedNumberOfRows)
        .isEqualTo(expectedNumberOfRows);
  }

  private void whenWeInsertOneSampleAuditData() {
    // when you insert/delete a record into/from table caz_vehicle_entrant.t_failed_identification_logs
    // 1 record will be added to caz_vehicle_entrant_audit.logged_actions
    insertOneRecordToFailedIdentificationLogsTable();
    insertOneRecordToLoggedActionsTable();
  }

  private void insertOneRecordToFailedIdentificationLogsTable() {
    String sql = "INSERT INTO caz_vehicle_entrant.t_failed_identification_logs (failedidentificationid,registrationnumber,exceptioncause,applicationversion,inserttimestamp) "
        + "VALUES ('ce3bbb9a-9bf9-11ea-bb37-0242ac130002','CAS310','unregconised fuel type','1.0',current_timestamp - '30 DAY'::interval)";
    jdbcTemplate.update(sql);
  }

  private void insertOneRecordToLoggedActionsTable() {
    String sql = "INSERT INTO caz_vehicle_entrant_audit.logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query,action_tstamp) "
        + "VALUES ('TG_TABLE_SCHEMA','TG_TABLE_NAME','session_user','I','original_data','new_data', 'current_query', current_timestamp - '18 MONTH'::interval)";
    jdbcTemplate.update(sql);
  }
}