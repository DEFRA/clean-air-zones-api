package uk.gov.caz.tariff.service;

import static uk.gov.caz.tariff.service.RepositoryUtils.safelyGetActiveChargeStartDate;

import com.google.common.annotations.VisibleForTesting;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.tariff.dto.InformationUrls;
import uk.gov.caz.tariff.dto.Rates;
import uk.gov.caz.tariff.dto.Tariff;

/**
 * A class that is responsible for managing tariff data ({@link Tariff} entities) in the postgres
 * database.
 */
@Repository
public class TariffRepository {

  @VisibleForTesting
  static final String SELECT_BY_CHARGE_DEFINITION_ID_SQL = "SELECT charge.clean_air_zone_id, "
      + "charge.caz_name, "
      + "charge.caz_class, "
      + "charge.charge_identifier, "
      + "charge.charging_disabled_vehicles, "
      + "charge.active_charge_start_time, "
      + "link.main_info_url, "
      + "link.exemption_url, "
      + "link.become_compliant_url, "
      + "link.boundary_url, "
      + "link.additional_info_url, "
      + "link.public_transport_options_url, "
      + "tar.hgv_entrant_fee, "
      + "tar.minibus_entrant_fee, "
      + "tar.car_entrant_fee, "
      + "tar.taxi_entrant_fee, "
      + "tar.phv_entrant_fee, "
      + "tar.bus_entrant_fee, "
      + "tar.motorcycle_ent_fee, "
      + "tar.coach_entrant_fee, "
      + "tar.van_entrant_fee, "
      + "tar.moped_entrant_fee "
      + "FROM t_tariff_definition tar, t_charge_definition charge, t_caz_link_detail link "
      + "WHERE tar.charge_definition_id = charge.charge_definition_id "
      + "AND link.charge_definition_id = charge.charge_definition_id "
      + "AND charge.clean_air_zone_id = ? "
      + "ORDER BY tar.tariff_id DESC "
      + "LIMIT 1";

  private static final TariffRowMapper MAPPER = new TariffRowMapper();

  private final JdbcTemplate jdbcTemplate;

  public TariffRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds {@link Tariff} by cleanAirZoneId.
   *
   * @param cleanAirZoneId UUID of zone for which tariffs will be fetched.
   * @return An {@link Optional} of {@link Tariff}
   */
  public Optional<Tariff> findByCleanAirZoneId(UUID cleanAirZoneId) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(SELECT_BY_CHARGE_DEFINITION_ID_SQL, MAPPER, cleanAirZoneId));
    } catch (EmptyResultDataAccessException exc) {
      return Optional.empty();
    }
  }

  /**
   * RowMapper for {@link Tariff}.
   */
  public static class TariffRowMapper implements RowMapper<Tariff> {

    @Override
    public Tariff mapRow(ResultSet rs, int i) throws SQLException {
      return Tariff.builder()
          .cleanAirZoneId(rs.getObject("clean_air_zone_id", UUID.class))
          .name(rs.getString("caz_name"))
          .tariffClass(rs.getString("caz_class").charAt(0))
          .chargeIdentifier(rs.getString("charge_identifier"))
          .chargingDisabledVehicles(rs.getBoolean("charging_disabled_vehicles"))
          .activeChargeStartDate(safelyGetActiveChargeStartDate(rs))
          .informationUrls(InformationUrls.builder()
              .becomeCompliant(rs.getString("become_compliant_url"))
              .mainInfo(rs.getString("main_info_url"))
              .exemptionOrDiscount(rs.getString("exemption_url"))
              .boundary(rs.getString("boundary_url"))
              .additionalInfo(rs.getString("additional_info_url"))
              .publicTransportOptions(rs.getString("public_transport_options_url"))
              .build())
          .rates(Rates.builder()
              .bus(rs.getBigDecimal("bus_entrant_fee"))
              .car(rs.getBigDecimal("car_entrant_fee"))
              .miniBus(rs.getBigDecimal("minibus_entrant_fee"))
              .coach(rs.getBigDecimal("coach_entrant_fee"))
              .taxi(rs.getBigDecimal("taxi_entrant_fee"))
              .phv(rs.getBigDecimal("phv_entrant_fee"))
              .hgv(rs.getBigDecimal("hgv_entrant_fee"))
              .van(rs.getBigDecimal("van_entrant_fee"))
              .motorcycle(rs.getBigDecimal("motorcycle_ent_fee"))
              .moped(rs.getBigDecimal("moped_entrant_fee"))
              .build())
          .build();
    }
  }
}