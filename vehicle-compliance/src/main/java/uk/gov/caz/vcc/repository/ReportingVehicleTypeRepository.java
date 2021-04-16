package uk.gov.caz.vcc.repository;

import com.google.common.annotations.VisibleForTesting;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.ReportingVehicleType;

@Repository
public class ReportingVehicleTypeRepository {

  public static final String SELECT_BY_VEHICLE_TYPE = "SELECT "
      + "ccaz_vehicle_type_id, "
      + "ccaz_vehicle_type "
      + "FROM caz_reporting.t_ccaz_vehicle_type "
      + "WHERE ccaz_vehicle_type=?";
  
  @VisibleForTesting
  public static final UUID UNRECOGNISED_VEHICLE_TYPE_ID = 
      UUID.fromString("6d697db0-01c1-4442-993d-e8747e3410c6");

  public static final VehicleTypeRowMapper ROW_MAPPER = new VehicleTypeRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor needed to build object.
   *
   * @param jdbcTemplate {@link JdbcTemplate}.
   */
  public ReportingVehicleTypeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds single entity of {@link ReportingVehicleType}. 
   * If none is found, {@link ReportingVehicleType} 
   * with unrecognised id is returned.
   *
   * @param vehicleType which will identify {@link ReportingVehicleType}.
   * @return single found {@link ReportingVehicleType}.
   */
  public ReportingVehicleType findVehicleTypeId(VehicleType vehicleType) {
    if (vehicleType == null) {
      return ReportingVehicleType.builder()
          .vehicleTypeId(UNRECOGNISED_VEHICLE_TYPE_ID)
          .build();
    }
    List<ReportingVehicleType> results = jdbcTemplate
        .query(SELECT_BY_VEHICLE_TYPE,
            preparedStatement -> preparedStatement.setString(1, vehicleType.toString()),
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new ApplicationRuntimeException(
          String.format("Not able to find Id for type approval: %s", vehicleType));
    }
    if (results.isEmpty()) {
      return ReportingVehicleType.builder()
          .vehicleTypeId(UNRECOGNISED_VEHICLE_TYPE_ID)
          .build();
    }
    return results.iterator().next();
  }

  static class VehicleTypeRowMapper implements RowMapper<ReportingVehicleType> {

    @Override
    public ReportingVehicleType mapRow(ResultSet resultSet, int i) throws SQLException {
      return ReportingVehicleType.builder()
          .vehicleType(resultSet.getString("ccaz_vehicle_type"))
          .vehicleTypeId(UUID.fromString(resultSet.getString("ccaz_vehicle_type_id")))
          .build();
    }
  }
}