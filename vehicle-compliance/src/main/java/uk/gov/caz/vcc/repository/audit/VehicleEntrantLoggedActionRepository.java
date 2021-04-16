package uk.gov.caz.vcc.repository.audit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class VehicleEntrantLoggedActionRepository {
  private final JdbcTemplate jdbcTemplate;
  
  /**
   * Remove log data that is older then the given date.
   *
   * @param inputDate given date
    */
  public int deleteLogsBeforeDate(LocalDate inputDate) {
    final String sql = "DELETE FROM caz_vehicle_entrant_audit.logged_actions "
        + "WHERE CAST (date_trunc('day', action_tstamp) AS date) <= ?";
    return jdbcTemplate.update(sql, new PreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps) throws SQLException {
        ps.setDate(1, java.sql.Date.valueOf(inputDate));
      }
    });
  }
}