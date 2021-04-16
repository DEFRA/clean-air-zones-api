package uk.gov.caz.vcc.repository;

import com.google.common.base.Strings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.vcc.domain.ReportingTypeApproval;


@Repository
public class ReportingTypeApprovalRepository {

  public static final String SELECT_BY_TYPE_APPROVAL = "SELECT "
      + "type_approval_id, "
      + "type_approval "
      + "FROM caz_reporting.t_type_approval "
      + "WHERE type_approval=?";
  
  private static final UUID UNRECOGNISED_TYPE_APPROVAL_ID = 
      UUID.fromString("c8f36849-9396-4ae7-810a-e5c862ec80bf");

  public static final TypeApprovalRowMapper ROW_MAPPER = new TypeApprovalRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor needed to build object.
   *
   * @param jdbcTemplate {@link JdbcTemplate}.
   */
  public ReportingTypeApprovalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds single entity of {@link ReportingTypeApproval}. If none is found, 
   * {@link ReportingTypeApproval} with unrecognised id is
   * returned.
   *
   * @param typeApproval which will identify {@link ReportingTypeApproval}.
   * @return single found {@link ReportingTypeApproval}.
   */
  public ReportingTypeApproval findTypeApprovalId(String typeApproval) {
    if (Strings.isNullOrEmpty(typeApproval)) {
      return ReportingTypeApproval.builder()
          .typeApprovalId(UNRECOGNISED_TYPE_APPROVAL_ID)
          .build();
    }
    List<ReportingTypeApproval> results = jdbcTemplate
        .query(SELECT_BY_TYPE_APPROVAL,
            preparedStatement -> preparedStatement.setString(1, typeApproval),
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new ApplicationRuntimeException(
          String.format("Not able to find Id for type approval: %s", typeApproval));
    }
    if (results.isEmpty()) {
      return ReportingTypeApproval.builder()
          .typeApprovalId(UNRECOGNISED_TYPE_APPROVAL_ID)
          .build();
    }
    return results.iterator().next();
  }

  static class TypeApprovalRowMapper implements RowMapper<ReportingTypeApproval> {

    @Override
    public ReportingTypeApproval mapRow(ResultSet resultSet, int i) throws SQLException {
      return ReportingTypeApproval.builder()
          .typeApproval(resultSet.getString("type_approval"))
          .typeApprovalId(UUID.fromString(resultSet.getString("type_approval_id")))
          .build();
    }
  }
}