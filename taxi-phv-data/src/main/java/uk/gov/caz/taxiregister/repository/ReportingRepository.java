package uk.gov.caz.taxiregister.repository;

import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.LicenceEvent;
import uk.gov.caz.taxiregister.util.JsonHelpers;

/**
 * A class that executes reporting queries against audit tables.
 */
@AllArgsConstructor
@Slf4j
@Repository
public class ReportingRepository {

  private static final RowMapper<String> LA_ROW_MAPPER = (rs, i) ->
      rs.getString("LICENCE_AUTHORITY_NAME");
  private static final LicenceEventRowMapper MAPPER = new LicenceEventRowMapper();

  private static final String ACTIVE_LICENCES_ON_DATE_BY_LA = "SELECT "
      // unique licence identifier
      + "new_data ->> 'taxi_phv_register_id' AS licence_id, "
      // VRM
      + "new_data ->> 'vrm' AS vrm "

      // CAN BE USEFUL FOR DEBUGGING:
      // date(new_data::json ->> 'licence_start_date') as start_date
      // date(new_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // only records with matching licence_authority_id
      + "AND (new_data ->> 'licence_authority_id')::integer = :licence_auth_id "
      // only inserts
      + "AND action = 'I' "
      // include only active licence for a given date
      + "AND :input_date >= custom_date_cast(new_data ->> 'licence_start_date') "
      + "AND :input_date <= custom_date_cast(new_data ->> 'licence_end_date') "
      // include only licences added before or on the given date
      + "AND date_trunc('day', action_tstamp)::date <= :input_date "

      // mirror query, but for *deletions* - this is to exclude licences which were *deleted*
      + "EXCEPT "

      + "SELECT "
      // unique licence identifier
      + "original_data ->> 'taxi_phv_register_id' AS licence_id, "
      // vrm
      + "original_data ->> 'vrm' AS vrm "

      // CAN BE USEFUL FOR DEBUGGING:
      // date(original_data::json ->> 'licence_start_date') as start_date
      // date(original_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // only records with matching licence_authority_id
      + "AND (original_data ->> 'licence_authority_id')::integer = :licence_auth_id "
      // only deletions
      + "AND action = 'D' "
      // include only active licence for a given date
      + "AND :input_date >= custom_date_cast(original_data ->> 'licence_start_date') "
      + "AND :input_date <= custom_date_cast(original_data ->> 'licence_end_date') "
      // include only licences added before or on the given date
      + "AND date_trunc('day', action_tstamp)::date <= :input_date";

  private static final String ACTIVE_LA_ON_DATE_BY_VRM_SQL = "SELECT "
      // unique licence identifier
      + "new_data::JSON ->> 'taxi_phv_register_id' AS licence_id, "
      // licence authority name (if present, null if absent)
      + "la.LICENCE_AUTHORITY_NAME "

      // CAN BE USEFUL FOR DEBUGGING:
      // new_data::json ->> 'licence_authority_id'     as la_id
      // date(new_data::json ->> 'licence_start_date') as start_date
      // date(new_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "LEFT OUTER JOIN T_MD_LICENSING_AUTHORITY la "
      + "ON la.LICENCE_AUTHORITY_ID = (new_data::JSON ->> 'licence_authority_id')::integer "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // select only records applicable to a given VRM
      + "AND (:vrm)::jsonb <@ new_data "
      // only inserts
      + "AND action = 'I' "
      // include only active licence for a given date
      + "AND :input_date >= date(new_data::JSON ->> 'licence_start_date') "
      + "AND :input_date <= date(new_data::JSON ->> 'licence_end_date') "

      // include only licences added before or on the given date
      + "AND date_trunc('day', action_tstamp)::date <= :input_date "

      // mirror query, but for *deletions* - this is to exclude licences which were *deleted*
      + "EXCEPT "

      + "SELECT "
      // unique licence identifier
      + "original_data::JSON ->> 'taxi_phv_register_id' AS licence_id, "
      // licence authority name (if present, null if absent)
      + "la.LICENCE_AUTHORITY_NAME "

      // CAN BE USEFUL FOR DEBUGGING"
      // original_data::json ->> 'licence_authority_id'     as la_id,
      // date(original_data::json ->> 'licence_start_date') as start_date,
      // date(original_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "LEFT OUTER JOIN T_MD_LICENSING_AUTHORITY la "
      + "ON la.LICENCE_AUTHORITY_ID = (original_data::JSON ->> 'licence_authority_id')::integer "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // select only records applicable to a given VRM
      + "AND (:vrm)::jsonb <@ original_data "
      // include only deletions
      + "AND action = 'D' "
      // include only active licence for a given date
      + "AND :input_date >= date(original_data::JSON ->> 'licence_start_date') "
      + "AND :input_date <= date(original_data::JSON ->> 'licence_end_date') "
      // include only licences added before or on the given date
      + "AND date_trunc('day', action_tstamp)::date <= :input_date";

  private static final String REPORTING_QUERY = "SELECT ala.action_tstamp as action_timestamp, "
      + "ala.action as db_action, "
      + newData("vrm") + " as vrm, "
      + "(" + newData("insert_timestmp") + ")::timestamp as insert_timestamp, "
      + "(" + newData("licence_start_date") + ")::date as licence_start_date, "
      + "(" + newData("licence_end_date") + ")::date as licence_end_date, "
      + newData("licence_authority_id") + " as licence_authority_id, "
      + newData("licence_plate_number") + " as licence_plate_number, "
      + newData("wheelchair_access_flag") + " as wheelchair_access_flag, "
      + newData("description") + " as description, "
      + "(" + newData("uploader_id") + ")::uuid as uploader_id "
      + "FROM audit.logged_actions ala "
      + "WHERE ala.table_name like 't_md_taxi_phv' "
      + "AND (ala.action = 'I' OR ala.action = 'U') "
      + "AND date_trunc('day', ala.action_tstamp)::date <= :reporting_end_date "
      // Union with DELETE type audit logs - they have only originalData column
      + "UNION "
      + "SELECT ala.action_tstamp as action_timestamp, "
      + "ala.action as db_action, "
      + originalData("vrm") + " as vrm, "
      + "(" + originalData("insert_timestmp") + ")::timestamp as insert_timestamp, "
      + "(" + originalData("licence_start_date") + ")::date as licence_start_date, "
      + "(" + originalData("licence_end_date") + ")::date as licence_end_date, "
      + originalData("licence_authority_id") + " as licence_authority_id, "
      + originalData("licence_plate_number") + " as licence_plate_number, "
      + originalData("wheelchair_access_flag") + " as wheelchair_access_flag, "
      + originalData("description") + " as description, "
      + "(" + originalData("uploader_id") + ")::uuid as uploader_id "
      + "FROM audit.logged_actions ala "
      + "WHERE ala.table_name like 't_md_taxi_phv' "
      + "AND (ala.action = 'D') "
      + "AND date_trunc('day', ala.action_tstamp)::date <= :reporting_end_date "
      + "ORDER BY action_timestamp ASC;";

  private static final String UNKNOWN_LICENSING_AUTHORITY = "UNKNOWN";

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final JsonHelpers jsonHelpers;

  /**
   * Returns licensing authorities (names) of active licences for a given VRM and date.
   *
   * @param vrm Vehicle registration mark.
   * @param date The date against which the check is performed.
   * @return A set of names of licensing authorities.
   */
  public Set<String> getLicensingAuthoritiesOfActiveLicencesForVrmOn(String vrm, LocalDate date) {
    List<String> names = namedParameterJdbcTemplate.query(ACTIVE_LA_ON_DATE_BY_VRM_SQL,
        toSqlParameterSource(vrm, date), LA_ROW_MAPPER);
    log.info("Got {} active licences from DB for VRN", names.size());
    return names.stream()
        .map(la -> la == null ? UNKNOWN_LICENSING_AUTHORITY : la)
        .collect(Collectors.toSet());
  }

  /**
   * Gets stream of events related to Licences. Stream is a complete timeline of events that
   * happened to `t_md_taxi_phv' table and is done by querying audit.logged_actions.
   *
   * @param reportingEndDate Reporting window end date which will be used to filter events.
   * @return stream of events related to Licences ordered from oldest to latest and filtered to only
   *     have ones that happened before {@code reportingEndDate}
   */
  public List<LicenceEvent> getLicenceEvents(LocalDate reportingEndDate) {
    return namedParameterJdbcTemplate
        .query(REPORTING_QUERY, toSqlParameterSourceWithReportingEndDate(reportingEndDate), MAPPER);
  }

  /**
   * Converts the passed parameters to {@link MapSqlParameterSource}.
   */
  private MapSqlParameterSource toSqlParameterSource(String vrm, LocalDate date) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("input_date", date, Types.DATE);
    mapSqlParameterSource.addValue("vrm", toJson(vrm), Types.VARCHAR);
    return mapSqlParameterSource;
  }

  /**
   * Converts the passed parameter to {@link MapSqlParameterSource}.
   */
  private MapSqlParameterSource toSqlParameterSourceWithReportingEndDate(
      LocalDate reportingEndDate) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("reporting_end_date", reportingEndDate, Types.DATE);
    return mapSqlParameterSource;
  }

  /**
   * Converts the passed vrm to a json object with 'vrm' as a key and {@code vrm} as a value.
   */
  private String toJson(String vrm) {
    return jsonHelpers.toJson(Collections.singletonMap("vrm", vrm));
  }

  /**
   * Forms select clause for json column with audit log new data.
   */
  private static String newData(String fieldName) {
    return "ala.new_data::JSON ->> '" + fieldName + "'";
  }

  /**
   * Forms select clause for json column with audit log original data.
   */
  private static String originalData(String fieldName) {
    return "ala.original_data::JSON ->> '" + fieldName + "'";
  }

  /**
   * Maps raw database rows into model {@link LicenceEvent} objects.
   */
  static class LicenceEventRowMapper implements RowMapper<LicenceEvent> {

    private static final ImmutableMap<String, Boolean> EXPECTED_BOOLEAN_VALUES =
        new ImmutableMap.Builder<String, Boolean>()
            .put("y", Boolean.TRUE)
            .put("n", Boolean.FALSE)
            .build();

    @Override
    public LicenceEvent mapRow(ResultSet rs, int i) throws SQLException {
      return LicenceEvent.builder()
          .vrm(rs.getString("vrm"))
          .action(rs.getString("db_action"))
          .eventTimestamp(rs.getTimestamp("action_timestamp").toLocalDateTime())
          .licenceStartDate(rs.getObject("licence_start_date", LocalDate.class))
          .licenceEndDate(rs.getObject("licence_end_date", LocalDate.class))
          .licencePlateNumber(rs.getString("licence_plate_number"))
          .licensingAuthorityId(rs.getInt("licence_authority_id"))
          .uploaderId(rs.getObject("uploader_id", UUID.class))
          .insertTimestamp(rs.getTimestamp("insert_timestamp").toLocalDateTime())
          .wheelchairAccessible(mapWheelchairAccessFlag(rs.getString("wheelchair_access_flag")))
          .description(rs.getString("description"))
          .build();
    }

    static Boolean mapWheelchairAccessFlag(String wheelchairAccessFlag) {
      return EXPECTED_BOOLEAN_VALUES.getOrDefault(wheelchairAccessFlag, null);
    }
  }
}
