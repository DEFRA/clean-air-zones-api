package uk.gov.caz.taxiregister.repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
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
  private static final RowMapper<String> VRM_ROW_MAPPER = (rs, i) ->
      rs.getString("VRM");

  private static final String ACTIVE_LICENCES_ON_DATE_BY_LA = "SELECT "
      // unique licence identifier
      + "new_data::JSON ->> 'taxi_phv_register_id' AS licence_id, "
      // VRM
      + "new_data::JSON ->> 'vrm' AS vrm "

      // CAN BE USEFUL FOR DEBUGGING:
      // date(new_data::json ->> 'licence_start_date') as start_date
      // date(new_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // only records with matching licence_authority_id
      + "AND (new_data::JSON ->> 'licence_authority_id')::integer = :licence_auth_id "
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
      // vrm
      + "original_data::JSON ->> 'vrm' AS vrm "

      // CAN BE USEFUL FOR DEBUGGING:
      // date(original_data::json ->> 'licence_start_date') as start_date
      // date(original_data::json ->> 'licence_end_date')   as end_date

      + "FROM audit.logged_actions "
      + "WHERE TABLE_NAME = 't_md_taxi_phv' "
      // only records with matching licence_authority_id
      + "AND (original_data::JSON ->> 'licence_authority_id')::integer = :licence_auth_id "
      // only deletions
      + "AND action = 'D' "
      // include only active licence for a given date
      + "AND :input_date >= date(original_data::JSON ->> 'licence_start_date') "
      + "AND :input_date <= date(original_data::JSON ->> 'licence_end_date') "
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
    log.info("Got {} active licences for {}", names.size(), vrm);
    return names.stream()
        .map(la -> la == null ? UNKNOWN_LICENSING_AUTHORITY : la)
        .collect(Collectors.toSet());
  }

  /**
   * Returns licences (represented by VRMs) which were active for a given licensing authority
   * (represented by {@code licensingAuthorityId}) on a given date.
   *
   * @param licensingAuthorityId The identifier of the licensing authority.
   * @param date The date against which the check is performed.
   * @return A {@link Set} of VRMs which had at least one active licence on a given date for a given
   *     licensing authority.
   */
  public Set<String> getActiveLicencesForLicensingAuthorityOn(int licensingAuthorityId,
      LocalDate date) {
    List<String> vrms = namedParameterJdbcTemplate.query(ACTIVE_LICENCES_ON_DATE_BY_LA,
        toSqlParameterSource(licensingAuthorityId, date), VRM_ROW_MAPPER);
    log.info("Got {} active licences for licensing authority {}", vrms.size(),
        licensingAuthorityId);
    return new HashSet<>(vrms);
  }

  /**
   * Converts the passed parameters to {@link MapSqlParameterSource}.
   */
  private MapSqlParameterSource toSqlParameterSource(int licensingAuthorityId, LocalDate date) {
    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("input_date", date, Types.DATE);
    mapSqlParameterSource.addValue("licence_auth_id", licensingAuthorityId,
        Types.INTEGER);
    return mapSqlParameterSource;
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
   * Converts the passed vrm to a json object with 'vrm' as a key and {@code vrm} as a value.
   */
  private String toJson(String vrm) {
    return jsonHelpers.toJson(Collections.singletonMap("vrm", vrm));
  }
}
