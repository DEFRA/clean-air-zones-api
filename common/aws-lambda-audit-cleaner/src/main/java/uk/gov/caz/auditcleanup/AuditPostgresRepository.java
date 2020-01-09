package uk.gov.caz.auditcleanup;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Types;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
class AuditPostgresRepository {

  @VisibleForTesting
  static final String DELETE_SQL = "DELETE FROM "
      + "audit.logged_actions "
      + "WHERE date_trunc('day', action_tstamp)::date < :input_date";

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  /**
   * Deletes audit events that happened before @date.
   */
  public void removeAuditEventsBeforeDate(LocalDate date) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("input_date", date, Types.DATE);

    namedParameterJdbcTemplate.update(DELETE_SQL, mapSqlParameterSource);
  }
}