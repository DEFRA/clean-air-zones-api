package uk.gov.caz.taxiregister.util;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;

public class AuditLogShaper {

  private static final String FORMATTER_PATTERN_DATETTIME = "yyyy-MM-dd_HHmmss";
  private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter
      .ofPattern(FORMATTER_PATTERN_DATETTIME);
  private static final String FORMATTER_PATTERN_DATE = "yyyy-MM-dd";
  private static final DateTimeFormatter FMT_DATE = DateTimeFormatter
      .ofPattern(FORMATTER_PATTERN_DATE);
  private static final String INSERT_MARKER = "I";
  private static final String DELETE_MARKER = "D";
  private static final String NO_DATA = null;

  private JdbcTemplate jdbcTemplate;
  private final LocalDateTime actionTimestamp;
  private String vrn;
  private String licensingAuthorityName;
  private LocalDate licenceStartDate;
  private LocalDate licenceEndDate;
  private LicenceInAuditLog licenceInAuditLog;
  private static int licenceId = 1;

  public AuditLogShaper(JdbcTemplate jdbcTemplate, String actionTimestamp) {
    this.jdbcTemplate = jdbcTemplate;
    this.actionTimestamp = dateTime(actionTimestamp);
  }

  public AuditLogShaper licenceFor(String vrn) {
    this.vrn = vrn;
    return this;
  }

  public AuditLogShaper in(String licensingAuthorityName) {
    this.licensingAuthorityName = licensingAuthorityName;
    return this;
  }

  public AuditLogShaper withStartAndEndDates(String start, String end) {
    licenceStartDate = date(start);
    licenceEndDate = date(end);
    return this;
  }

  public AuditLogShaper licence(LicenceInAuditLog licenceInAuditLog) {
    this.licenceInAuditLog = licenceInAuditLog;
    return this;
  }

  public LicenceInAuditLog wasUploaded() {
    return new LicenceInAuditLog(putInsertIntoAuditLog());
  }

  public void wasDeleted() {
    putDeleteIntoAuditLog(licenceInAuditLog.getOriginalData());
  }

  private String putInsertIntoAuditLog() {
    String newData = formJsonWithNewData();
    putIntoAuditLog(INSERT_MARKER, () -> newData);
    return newData;
  }

  private void putDeleteIntoAuditLog(String originalData) {
    putIntoAuditLog(DELETE_MARKER, () -> originalData);
  }

  private void putIntoAuditLog(String operationMarker, Supplier<String> dataSupplier) {
    String insertSql = "INSERT INTO audit.logged_actions "
        + "(schema_name, table_name, user_name, action_tstamp, action, original_data, new_data, query) "
        + "values ('public', 't_md_taxi_phv', 'ntr', ?, ?, to_jsonb(?::jsonb), to_jsonb(?::jsonb), 'query_not_important')";

    jdbcTemplate.update(
        insertSql,
        actionTimestamp,
        operationMarker,
        operationMarker.equals(DELETE_MARKER) ? dataSupplier.get() : NO_DATA,
        operationMarker.equals(INSERT_MARKER) ? dataSupplier.get() : NO_DATA
    );
  }

  private String formJsonWithNewData() {
    return String.format(
        "{\"vrm\": \"%s\", "
            + "\"description\": \"taxi\", " // does not matter
            + "\"uploader_id\": \"06545fcc-42d8-412b-9893-3687799ee5fa\", " // does not matter
            + "\"insert_timestmp\": \"%s\", "
            + "\"licence_end_date\": \"%s\", "
            + "\"licence_start_date\": \"%s\", "
            + "\"licence_authority_id\": %s, "
            + "\"licence_plate_number\": \"olAOT\", " // does not matter
            + "\"taxi_phv_register_id\": %s, "
            + "\"wheelchair_access_flag\": \"y\"}",  // does not matter
        vrn,
        actionTimestamp,
        licenceEndDate,
        licenceStartDate,
        LA_NAME_TO_ID.get(licensingAuthorityName),
        licenceId++);
  }

  private LocalDateTime dateTime(String dateTime) {
    return LocalDateTime.parse(dateTime, FMT_DATETIME);
  }

  private LocalDate date(String date) {
    return LocalDate.parse(date, FMT_DATE);
  }

  private static Map<String, Integer> LA_NAME_TO_ID = ImmutableMap.of(
      "Birmingham", 1,
      "Leeds", 2
  );

  public void licenceAuthorityWasDeleted(String licensingAuthorityName) {
    jdbcTemplate.update("DELETE FROM T_MD_LICENSING_AUTHORITY WHERE LICENCE_AUTHORITY_NAME = ?",
        preparedStatement -> preparedStatement.setString(1, licensingAuthorityName));
  }
}
