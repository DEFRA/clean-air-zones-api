package uk.gov.caz.tariff.service;

import static uk.gov.caz.tariff.service.RepositoryUtils.safelyGetActiveChargeStartDate;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;

/**
 * A class that is responsible for managing cleanAirZone data ({@link CleanAirZoneDto} entities) in
 * the postgres database.
 */
@Repository
public class CleanAirZonesRepository {

  @VisibleForTesting
  static final String SELECT_ALL_SQL = "SELECT charge.clean_air_zone_id, "
      + "charge.caz_name, "
      + "charge.active_charge_start_time, "
      + "charge.caz_operator_name, "
      + "charge.direct_debit_enabled, "
      + "link.boundary_url, "
      + "link.exemption_url, "
      + "link.main_info_url "
      + "FROM t_charge_definition charge, t_caz_link_detail link "
      + "WHERE link.charge_definition_id = charge.charge_definition_id ";

  private static final CleanAirZoneRowMapper MAPPER = new CleanAirZoneRowMapper();

  private final JdbcTemplate jdbcTemplate;

  public CleanAirZonesRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds every {@link CleanAirZoneDto}.
   *
   * @return {@link CleanAirZonesDto} list of {@link CleanAirZoneDto}
   */
  public CleanAirZonesDto findAll() {
    return CleanAirZonesDto.builder().cleanAirZones(jdbcTemplate.query(SELECT_ALL_SQL, MAPPER))
        .build();
  }

  /**
   * RowMapper for {@link CleanAirZoneDto}.
   */
  public static class CleanAirZoneRowMapper implements RowMapper<CleanAirZoneDto> {

    @Override
    public CleanAirZoneDto mapRow(ResultSet rs, int i) throws SQLException {
      return CleanAirZoneDto.builder()
          .cleanAirZoneId(rs.getObject("clean_air_zone_id", UUID.class))
          .name(rs.getString("caz_name"))
          .boundaryUrl(URI.create(rs.getString("boundary_url")))
          .exemptionUrl(URI.create(rs.getString("exemption_url")))
          .mainInfoUrl(URI.create(rs.getString("main_info_url")))
          .activeChargeStartDate(safelyGetActiveChargeStartDate(rs))
          .operatorName(rs.getString("caz_operator_name"))
          .directDebitEnabled(rs.getBoolean("direct_debit_enabled"))
          .build();
    }
  }
}