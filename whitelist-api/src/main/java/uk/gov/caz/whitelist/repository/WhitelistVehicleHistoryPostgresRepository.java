package uk.gov.caz.whitelist.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;

/**
 * A class that is responsible for managing vehicle's licences historical data ({@link
 * WhitelistVehicleHistory} entities) in the postgres database.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WhitelistVehicleHistoryPostgresRepository {

  private static final ZoneId LONDON_ZONE_ID = ZoneId.of("Europe/London");
  @VisibleForTesting
  protected static final ImmutableMap<String, String> EXPECTED_ACTION_VALUES =
      new ImmutableMap.Builder<String, String>()
          .put("U", "Updated")
          .put("I", "Created")
          .put("D", "Removed")
          .build();

  private static final String ACTION = "action";
  private static final String ACTION_TSTAMP = ACTION + "_tstamp";
  private static final String MODIFIER_ID_NAME = "modifier_id";
  private static final String REASON_UPDATED_NAME = "reason_updated";
  private static final String MANUFACTURER_NAME = "manufacturer";
  private static final String CATEGORY_NAME = "category";
  private static final String VRN_NAME = "vrn";
  private static final String MODIFIER_EMAIL_NAME = "modifier_email";

  private static final String NEW_DATA_PREFIX = "n_";
  private static final String ORIGINAL_DATA_PREFIX = "o_";

  private static final String NEW_DATA_REASON_UPDATED_NAME =
      NEW_DATA_PREFIX + REASON_UPDATED_NAME;
  private static final String NEW_DATA_MANUFACTURER_NAME = NEW_DATA_PREFIX + MANUFACTURER_NAME;
  private static final String NEW_DATA_CATEGORY_NAME = NEW_DATA_PREFIX + CATEGORY_NAME;

  private static final String ORIGINAL_REASON_UPDATED_NAME =
      ORIGINAL_DATA_PREFIX + REASON_UPDATED_NAME;
  private static final String ORIGINAL_DATA_MANUFACTURER_NAME =
      ORIGINAL_DATA_PREFIX + MANUFACTURER_NAME;
  private static final String ORIGINAL_DATA_CATEGORY_NAME =
      ORIGINAL_DATA_PREFIX + CATEGORY_NAME;

  private static final String SELECT_FIELDS = "SELECT a." + ACTION_TSTAMP + ", "
      + "a." + ACTION + ", "
      + "a." + MODIFIER_ID_NAME + ", "
      + "a." + MODIFIER_EMAIL_NAME + ", "
      + newData(REASON_UPDATED_NAME) + " AS " + NEW_DATA_REASON_UPDATED_NAME + ", "
      + newData(MANUFACTURER_NAME) + " AS " + NEW_DATA_MANUFACTURER_NAME + ", "
      + newData(CATEGORY_NAME) + " AS " + NEW_DATA_CATEGORY_NAME + ", "
      + originalData(REASON_UPDATED_NAME) + " AS " + ORIGINAL_REASON_UPDATED_NAME + ", "
      + originalData(MANUFACTURER_NAME) + " AS " + ORIGINAL_DATA_MANUFACTURER_NAME + ", "
      + originalData(CATEGORY_NAME) + " AS " + ORIGINAL_DATA_CATEGORY_NAME + " ";

  public static final String SELECT_COUNT = "SELECT COUNT(a." + ACTION_TSTAMP + ") ";

  private static final String QUERY_SUFFIX = "FROM  caz_whitelist_vehicles_audit.logged_actions a "
      + "WHERE a." + VRN_NAME + " = ? "
      + "AND a." + ACTION_TSTAMP + " >= ? "
      + "AND a." + ACTION_TSTAMP + " <= ? ";

  private static final String PAGING_SUFFIX = "ORDER BY a." + ACTION_TSTAMP + " DESC "
      + "LIMIT ? "
      + "OFFSET ? ";

  @VisibleForTesting
  static final String SELECT_BY_VRN_HISTORY_IN_RANGE = SELECT_FIELDS + QUERY_SUFFIX + PAGING_SUFFIX;

  @VisibleForTesting
  static final String SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT = SELECT_COUNT + QUERY_SUFFIX;

  private static String newData(String fieldName) {
    return "a.new_data ->> '" + fieldName + "'";
  }

  private static String originalData(String fieldName) {
    return "a.original_data ->> '" + fieldName + "'";
  }

  private static final LicenceHistoryRowMapper MAPPER = new LicenceHistoryRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Finds all {@link WhitelistVehicleHistory} entities for a given vrn and date range.
   *
   * @param vrn for which all matching licences are returned
   * @return {@link List} of all {@link WhitelistVehicleHistory} which matches passed vrn and date
   *     range.
   */
  public List<WhitelistVehicleHistory> findByVrnInRange(String vrn, LocalDateTime startDateTime,
      LocalDateTime endDateTime, long pageSize, long pageNumber) {
    return jdbcTemplate.query(
        SELECT_BY_VRN_HISTORY_IN_RANGE,
        preparedStatement -> {
          int i = 0;
          preparedStatement.setString(++i, vrn);
          preparedStatement.setObject(++i, startDateTime);
          preparedStatement.setObject(++i, endDateTime);
          preparedStatement.setObject(++i, pageSize);
          preparedStatement.setObject(++i, pageNumber * pageSize);
        },
        MAPPER
    );
  }

  /**
   * Count all {@link WhitelistVehicleHistory} entities for a given vrn and date range.
   *
   * @param vrn for which all matching licences are returned
   * @return {@link Long} of all histories which matches passed vrn and date range.
   */
  public Long count(String vrn, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    List<Object> ts = Arrays.asList(vrn, startDateTime, endDateTime);
    return jdbcTemplate.queryForObject(
        SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT,
        ts.toArray(),
        Long.class
    );
  }

  @VisibleForTesting
  static class LicenceHistoryRowMapper implements RowMapper<WhitelistVehicleHistory> {

    private static final String DELETE_ACTION = "D";

    @Override
    public WhitelistVehicleHistory mapRow(ResultSet rs, int i) throws SQLException {
      String action = rs.getString(ACTION);
      boolean isRemoved = DELETE_ACTION.equals(action);
      return WhitelistVehicleHistory.builder()
          .modifyDate(Optional.ofNullable(rs.getObject(ACTION_TSTAMP, OffsetDateTime.class))
              .map(offsetDateTime -> offsetDateTime.atZoneSameInstant(LONDON_ZONE_ID).toLocalDate())
              .orElse(null))
          .action(mapAction(action))
          .reasonUpdated(rs.getString(
              isRemoved ? ORIGINAL_REASON_UPDATED_NAME : NEW_DATA_REASON_UPDATED_NAME))
          .manufacturer(
              rs.getString(
                  isRemoved ? ORIGINAL_DATA_MANUFACTURER_NAME : NEW_DATA_MANUFACTURER_NAME))
          .category(
              rs.getString(
                  isRemoved ? ORIGINAL_DATA_CATEGORY_NAME : NEW_DATA_CATEGORY_NAME))
          .modifierId(rs.getString(MODIFIER_ID_NAME))
          .modifierEmail(rs.getString(MODIFIER_EMAIL_NAME))
          .build();
    }

    static String mapAction(String wheelchairAccessFlag) {
      return EXPECTED_ACTION_VALUES.getOrDefault(wheelchairAccessFlag, null);
    }
  }
}