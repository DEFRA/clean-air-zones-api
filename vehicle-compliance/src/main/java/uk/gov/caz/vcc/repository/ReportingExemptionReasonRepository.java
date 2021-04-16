package uk.gov.caz.vcc.repository;

import com.google.common.base.Strings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.vcc.domain.ReportingExemptionReason;

@Slf4j
@Repository
public class ReportingExemptionReasonRepository {

  public static final String SELECT_BY_EXEMPTION_REASON = "SELECT "
      + "exemption_reason_id, "
      + "exemption_reason "
      + "FROM caz_reporting.t_exemption_reason "
      + "WHERE LOWER(exemption_reason)=?";
  
  private static final UUID UNRECOGNISED_EXEMPTION_REASON_ID = 
      UUID.fromString("73fb59b2-07a8-43f2-96f9-64c2d25a9f66");

  public static final ExemptionReasonRowMapper ROW_MAPPER = new ExemptionReasonRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor needed to build object.
   *
   * @param jdbcTemplate {@link JdbcTemplate}.
   */
  public ReportingExemptionReasonRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds single entity of {@link ReportingExemptionReason}. If none is found, 
   * {@link ReportingExemptionReason} with unrecognised is returned.
   *
   * @param exemptionReason which will identify {@link ReportingExemptionReason}.
   * @return single found {@link ReportingExemptionReason}.
   */
  public ReportingExemptionReason findExemptionReasonId(String exemptionReason) {
    if (Strings.isNullOrEmpty(exemptionReason)) {
      return ReportingExemptionReason.builder()
          .exemptionReasonId(UNRECOGNISED_EXEMPTION_REASON_ID)
          .build();
    }
    List<ReportingExemptionReason> results = jdbcTemplate
        .query(SELECT_BY_EXEMPTION_REASON,
            preparedStatement -> preparedStatement.setString(1, exemptionReason.toLowerCase()),
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new ApplicationRuntimeException(
          String.format("Not able to find Id for exemption reason: %s", exemptionReason));
    }
    if (results.isEmpty()) {
      return ReportingExemptionReason.builder()
          .exemptionReasonId(UNRECOGNISED_EXEMPTION_REASON_ID)
          .build();
    }
    return results.iterator().next();
  }

  static class ExemptionReasonRowMapper implements RowMapper<ReportingExemptionReason> {

    @Override
    public ReportingExemptionReason mapRow(ResultSet resultSet, int i) throws SQLException {
      return ReportingExemptionReason.builder()
          .exemptionReason(resultSet.getString("exemption_reason"))
          .exemptionReasonId(UUID.fromString(resultSet.getString("exemption_reason_id")))
          .build();
    }
  }
}