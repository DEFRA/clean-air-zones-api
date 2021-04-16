package uk.gov.caz.taxiregister.repository;

import static uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository.LicenceRowMapper.mapWheelchairAccessFlag;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;

/**
 * A class that is responsible for managing vehicle's licences historical data ({@link
 * TaxiPhvVehicleLicenceHistory} entities) in the postgres database.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaxiPhvLicenceHistoryPostgresRepository {

  @VisibleForTesting
  protected static final ImmutableMap<String, String> EXPECTED_ACTION_VALUES =
      new ImmutableMap.Builder<String, String>()
          .put("U", "Updated")
          .put("I", "Created")
          .put("D", "Removed")
          .build();

  public static final ZoneId LONDON_ZONE_ID = ZoneId.of("Europe/London");

  private static final String LICENCE_AUTHORITY_NAME = "licence_authority_name";
  private static final String ACTION = "action";
  private static final String ACTION_TSTAMP = ACTION + "_tstamp";
  private static final String LICENCE_PLATE_NUMBER = "licence_plate_number";
  private static final String LICENCE_START_DATE = "licence_start_date";
  private static final String LICENCE_END_DATE = "licence_end_date";
  private static final String WHEELCHAIR_ACCESS_FLAG = "wheelchair_access_flag";
  private static final String LICENCE_AUTHORITY_ID = "licence_authority_id";
  private static final String VRM = "vrm";

  private static final String NEW_DATA_PREFIX = "n_";
  private static final String ORIGINAL_DATA_PREFIX = "o_";

  private static final String NEW_DATA_LICENCE_AUTHORITY_NAME =
      NEW_DATA_PREFIX + LICENCE_AUTHORITY_NAME;
  private static final String NEW_DATA_LICENCE_PLATE_NUMBER =
      NEW_DATA_PREFIX + LICENCE_PLATE_NUMBER;
  private static final String NEW_DATA_LICENCE_START_DATE = NEW_DATA_PREFIX + LICENCE_START_DATE;
  private static final String NEW_DATA_LICENCE_END_DATE = NEW_DATA_PREFIX + LICENCE_END_DATE;
  private static final String NEW_DATA_WHEELCHAIR_ACCESS_FLAG =
      NEW_DATA_PREFIX + WHEELCHAIR_ACCESS_FLAG;

  private static final String ORIGINAL_DATA_LICENCE_AUTHORITY_NAME =
      ORIGINAL_DATA_PREFIX + LICENCE_AUTHORITY_NAME;
  private static final String ORIGINAL_DATA_LICENCE_PLATE_NUMBER =
      ORIGINAL_DATA_PREFIX + LICENCE_PLATE_NUMBER;
  private static final String ORIGINAL_DATA_LICENCE_START_DATE =
      ORIGINAL_DATA_PREFIX + LICENCE_START_DATE;
  private static final String ORIGINAL_DATA_LICENSE_END_DATE =
      ORIGINAL_DATA_PREFIX + LICENCE_END_DATE;
  private static final String ORIGINAL_DATA_WHEELCHAIR_ACCESS_FLAG =
      ORIGINAL_DATA_PREFIX + WHEELCHAIR_ACCESS_FLAG;

  private static final String SELECT_FIELDS = "(SELECT a." + ACTION_TSTAMP + ", "
      + "a." + ACTION + ", "
      + "la." + LICENCE_AUTHORITY_NAME + " AS " + NEW_DATA_LICENCE_AUTHORITY_NAME + ", "
      + "dla." + LICENCE_AUTHORITY_NAME + " AS " + ORIGINAL_DATA_LICENCE_AUTHORITY_NAME + ", "
      + newData(LICENCE_PLATE_NUMBER) + " AS " + NEW_DATA_LICENCE_PLATE_NUMBER + ", "
      + "(" + newData(LICENCE_START_DATE) + ")::date AS " + NEW_DATA_LICENCE_START_DATE + ", "
      + "(" + newData(LICENCE_END_DATE) + ")::date AS " + NEW_DATA_LICENCE_END_DATE + ", "
      + newData(WHEELCHAIR_ACCESS_FLAG) + " AS " + NEW_DATA_WHEELCHAIR_ACCESS_FLAG + ", "
      + originalData(LICENCE_PLATE_NUMBER) + " AS " + ORIGINAL_DATA_LICENCE_PLATE_NUMBER + ", "
      + "(" + originalData(LICENCE_START_DATE) + ")::date AS " + ORIGINAL_DATA_LICENCE_START_DATE
      + ", "
      + "(" + originalData(LICENCE_END_DATE) + ")::date AS " + ORIGINAL_DATA_LICENSE_END_DATE + ", "
      + originalData(WHEELCHAIR_ACCESS_FLAG) + " AS " + ORIGINAL_DATA_WHEELCHAIR_ACCESS_FLAG + " ";

  public static final String SELECT_TSTAMP = "(SELECT a." + ACTION_TSTAMP + " ";
  
  public static final String SELECT_COUNT = "SELECT COUNT(query." + ACTION_TSTAMP + ") ";

  private static final String QUERY_SUFFIX_NEW_DATA = "FROM  audit.logged_actions a "
      + "LEFT JOIN  public.t_md_licensing_authority dla "
      + "   ON dla." + LICENCE_AUTHORITY_ID + " = (" + originalData(LICENCE_AUTHORITY_ID)
      + ")::INTEGER "
      + "LEFT JOIN  public.t_md_licensing_authority la "
      + "   ON la." + LICENCE_AUTHORITY_ID + " = (" + newData(LICENCE_AUTHORITY_ID)
      + ")::INTEGER "
      + "WHERE " + newData(VRM) + " = ? "
      + "AND a." + ACTION_TSTAMP + " >= ? "
      + "AND a." + ACTION_TSTAMP + " <= ? "
      + "AND a.table_name = 't_md_taxi_phv') ";
  
  private static final String QUERY_SUFFIX_ORIGINAL_DATA = "FROM  audit.logged_actions a "
      + "LEFT JOIN  public.t_md_licensing_authority dla "
      + "   ON dla." + LICENCE_AUTHORITY_ID + " = (" + originalData(LICENCE_AUTHORITY_ID)
      + ")::INTEGER "
      + "LEFT JOIN  public.t_md_licensing_authority la "
      + "   ON la." + LICENCE_AUTHORITY_ID + " = (" + newData(LICENCE_AUTHORITY_ID)
      + ")::INTEGER "
      + "WHERE " + originalData(VRM) + " = ? "
      + "AND a." + ACTION_TSTAMP + " >= ? "
      + "AND a." + ACTION_TSTAMP + " <= ? "
      + "AND a.table_name = 't_md_taxi_phv') ";

  private static final String PAGING_SUFFIX = "ORDER BY " + ACTION_TSTAMP + " DESC "
      + "LIMIT ? "
      + "OFFSET ? ";

  @VisibleForTesting
  static final String SELECT_BY_VRN_HISTORY_IN_RANGE = SELECT_FIELDS + QUERY_SUFFIX_NEW_DATA 
      + " UNION " + SELECT_FIELDS + QUERY_SUFFIX_ORIGINAL_DATA + PAGING_SUFFIX;

  @VisibleForTesting
  static final String SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT = SELECT_COUNT 
      + " FROM (" + SELECT_TSTAMP + QUERY_SUFFIX_NEW_DATA + " UNION "
      + SELECT_TSTAMP + QUERY_SUFFIX_ORIGINAL_DATA + ") AS query";

  private static String newData(String fieldName) {
    return "a.new_data ->> '" + fieldName + "'";
  }

  private static String originalData(String fieldName) {
    return "a.original_data ->> '" + fieldName + "'";
  }

  private static final LicenceHistoryRowMapper MAPPER = new LicenceHistoryRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Finds all {@link TaxiPhvVehicleLicenceHistory} entities for a given vrm and date range.
   *
   * @param vrm for which all matching licences are returned
   * @return {@link List} of all {@link TaxiPhvVehicleLicenceHistory} which matches passed vrm and
   *     date range.
   */
  public List<TaxiPhvVehicleLicenceHistory> findByVrmInRange(String vrm, LocalDateTime startDate,
      LocalDateTime endDate, long pageSize, long pageNumber) {
    return jdbcTemplate.query(
        SELECT_BY_VRN_HISTORY_IN_RANGE,
        preparedStatement -> {
          int i = 0;
          preparedStatement.setString(++i, vrm);
          preparedStatement.setObject(++i, startDate);
          preparedStatement.setObject(++i, endDate);
          preparedStatement.setString(++i, vrm);
          preparedStatement.setObject(++i, startDate);
          preparedStatement.setObject(++i, endDate);
          preparedStatement.setObject(++i, pageSize);
          preparedStatement.setObject(++i, pageNumber * pageSize);
        },
        MAPPER
    );
  }

  /**
   * Count all {@link TaxiPhvVehicleLicenceHistory} entities for a given vrm and date range.
   *
   * @param vrm for which all matching licences are returned
   * @return {@link Long} of all histories which matches passed vrm and date range.
   */
  public Long count(String vrm, LocalDateTime startDate, LocalDateTime endDate) {
    List<Object> ts = Arrays.asList(vrm, startDate, endDate, vrm, startDate, endDate);
    return jdbcTemplate.queryForObject(
        SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT,
        ts.toArray(),
        Long.class
    );
  }

  @VisibleForTesting
  static class LicenceHistoryRowMapper implements RowMapper<TaxiPhvVehicleLicenceHistory> {

    private static final String DELETE_ACTION = "D";

    @Override
    public TaxiPhvVehicleLicenceHistory mapRow(ResultSet rs, int i) throws SQLException {
      String action = rs.getString(ACTION);
      boolean isRemoved = DELETE_ACTION.equals(action);
      return TaxiPhvVehicleLicenceHistory.builder()
          .modifyDate(Optional.ofNullable(rs.getObject(ACTION_TSTAMP, OffsetDateTime.class))
              .map(offsetDateTime -> offsetDateTime.atZoneSameInstant(LONDON_ZONE_ID)
                  .toLocalDate()).orElse(null))
          .action(mapAction(action))
          .licensingAuthorityName(rs.getString(
              isRemoved ? ORIGINAL_DATA_LICENCE_AUTHORITY_NAME : NEW_DATA_LICENCE_AUTHORITY_NAME))
          .licenceStartDate(
              rs.getObject(
                  isRemoved ? ORIGINAL_DATA_LICENCE_START_DATE : NEW_DATA_LICENCE_START_DATE,
                  LocalDate.class))
          .licenceEndDate(
              rs.getObject(
                  isRemoved ? ORIGINAL_DATA_LICENSE_END_DATE : NEW_DATA_LICENCE_END_DATE,
                  LocalDate.class))
          .licencePlateNumber(
              rs.getString(
                  isRemoved ? ORIGINAL_DATA_LICENCE_PLATE_NUMBER : NEW_DATA_LICENCE_PLATE_NUMBER))
          .wheelchairAccessible(mapWheelchairAccessFlag(
              rs.getString(
                  isRemoved ? ORIGINAL_DATA_WHEELCHAIR_ACCESS_FLAG
                      : NEW_DATA_WHEELCHAIR_ACCESS_FLAG)))
          .build();
    }

    static String mapAction(String wheelchairAccessFlag) {
      return EXPECTED_ACTION_VALUES.getOrDefault(wheelchairAccessFlag, null);
    }
  }
}