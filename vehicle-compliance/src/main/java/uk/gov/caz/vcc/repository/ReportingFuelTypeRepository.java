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
import uk.gov.caz.vcc.domain.ReportingFuelType;

@Repository
public class ReportingFuelTypeRepository {

  public static final String SELECT_BY_FUEL_TYPE = "SELECT "
      + "fuel_type_id, "
      + "fuel_type "
      + "FROM caz_reporting.t_fuel_type "
      + "WHERE fuel_type=?";
  
  private static final UUID UNRECOGNISED_FUEL_TYPE_ID = 
      UUID.fromString("01e47366-b9a4-4e1a-99f4-b74dc501cd14");

  public static final FuelTypeRowMapper ROW_MAPPER = new FuelTypeRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor needed to build object.
   *
   * @param jdbcTemplate {@link JdbcTemplate}.
   */
  public ReportingFuelTypeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds single entity of {@link ReportingFuelType}. If none is found, {@link ReportingFuelType} 
   * with unrecognised Id is returned.
   *
   * @param fuelType which will identify {@link ReportingFuelType}.
   * @return single found {@link ReportingFuelType}.
   */
  public ReportingFuelType findFuelTypeId(String fuelType) {
    if (Strings.isNullOrEmpty(fuelType)) {
      return ReportingFuelType.builder()
          .fuelTypeId(UNRECOGNISED_FUEL_TYPE_ID)
          .build();
    }
    List<ReportingFuelType> results = jdbcTemplate
        .query(SELECT_BY_FUEL_TYPE,
            preparedStatement -> preparedStatement.setString(1, fuelType.toLowerCase()),
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new ApplicationRuntimeException(
          String.format("Not able to find Id for fuel type: %s", fuelType));
    }
    if (results.isEmpty()) {
      return ReportingFuelType.builder()
          .fuelTypeId(UNRECOGNISED_FUEL_TYPE_ID)
          .build();
    }
    return results.iterator().next();
  }

  static class FuelTypeRowMapper implements RowMapper<ReportingFuelType> {

    @Override
    public ReportingFuelType mapRow(ResultSet resultSet, int i) throws SQLException {
      return ReportingFuelType.builder()
          .fuelType(resultSet.getString("fuel_type"))
          .fuelTypeId(UUID.fromString(resultSet.getString("fuel_type_id")))
          .build();
    }
  }
}