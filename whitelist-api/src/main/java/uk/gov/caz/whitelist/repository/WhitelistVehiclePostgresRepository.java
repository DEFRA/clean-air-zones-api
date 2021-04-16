package uk.gov.caz.whitelist.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.whitelist.model.WhitelistVehicle;

@Slf4j
@Repository
public class WhitelistVehiclePostgresRepository {

  @VisibleForTesting
  static final String INSERT_OR_UPDATE_SQL =
      "INSERT INTO caz_whitelist_vehicles.t_whitelist_vehicles("
          + "reason_updated, "
          + "manufacturer, "
          + "uploader_id, "
          + "uploader_email, "
          + "category, "
          + "exempt, "
          + "compliant, "
          + "vrn, "
          + "insert_timestamp) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, now()) "
          + "ON CONFLICT (vrn) "
          + "DO UPDATE SET "
          + "reason_updated = excluded.reason_updated, "
          + "manufacturer = excluded.manufacturer, "
          + "uploader_id = excluded.uploader_id, "
          + "uploader_email = excluded.uploader_email, "
          + "category = excluded.category, "
          + "exempt = excluded.exempt, "
          + "compliant = excluded.compliant,"
          + "update_timestamp = excluded.update_timestamp ";

  public static final String SELECT_BY_VRN_QUERY = "SELECT "
      + "vrn, "
      + "category, "
      + "exempt, "
      + "compliant, "
      + "manufacturer, "
      + "reason_updated, "
      + "update_timestamp, "
      + "insert_timestamp, "
      + "uploader_id, "
      + "uploader_email "
      + "FROM caz_whitelist_vehicles.t_whitelist_vehicles "
      + "WHERE vrn=?";

  public static final String SELECT_ALL_SQL = "SELECT "
      + "vrn, "
      + "category, "
      + "exempt, "
      + "compliant, "
      + "manufacturer, "
      + "reason_updated, "
      + "update_timestamp, "
      + "insert_timestamp, "
      + "uploader_id, "
      + "uploader_email "
      + "FROM caz_whitelist_vehicles.t_whitelist_vehicles";

  public static final String DELETE_SQL = "DELETE FROM caz_whitelist_vehicles.t_whitelist_vehicles "
      + "WHERE vrn in (:vrns)";

  public static final WhitelistVehicleRowMapper ROW_MAPPER = new WhitelistVehicleRowMapper();

  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final int updateBatchSize;

  /**
   * Constructor needed to build object.
   *
   * @param jdbcTemplate {@link JdbcTemplate}.
   * @param namedParameterJdbcTemplate {@link NamedParameterJdbcTemplate}
   * @param updateBatchSize size of batch to insert/update.
   */
  public WhitelistVehiclePostgresRepository(JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      @Value("${application.jdbc.updateBatchSize:100}") int updateBatchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    this.updateBatchSize = updateBatchSize;
  }

  /**
   * Finds single entity of {@link WhitelistVehicle}. If none is found it returns FALSE.
   *
   * @param vrn which will identify {@link WhitelistVehicle}.
   * @return a boolean value specifying if .
   */
  public boolean exists(String vrn) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<WhitelistVehicle> results = jdbcTemplate
        .query(SELECT_BY_VRN_QUERY,
            preparedStatement -> preparedStatement.setString(1, vrn),
            ROW_MAPPER
        );

    return !results.isEmpty();
  }

  /**
   * Inserts passed set of {@link WhitelistVehicle} into the database in batches. The size of a
   * single batch sits in {@code application.jdbc.updateBatchSize} application property.
   *
   * @param whitelistVehicles A set of vehicles that will be inserted in the database.
   */
  public void saveOrUpdate(Set<WhitelistVehicle> whitelistVehicles) {
    Iterable<List<WhitelistVehicle>> batches = Iterables
        .partition(whitelistVehicles, updateBatchSize);
    for (List<WhitelistVehicle> batch : batches) {
      jdbcTemplate
          .batchUpdate(INSERT_OR_UPDATE_SQL, new WhitelistBatchPreparedStatementSetter(batch));
    }
  }

  /**
   * Finds single entity of {@link WhitelistVehicle}. If none is found, {@link Optional#empty()} is
   * returned.
   *
   * @param vrn which will identify {@link WhitelistVehicle}.
   * @return single found {@link WhitelistVehicle}.
   */
  public Optional<WhitelistVehicle> findOneByVrn(String vrn) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");

    List<WhitelistVehicle> results = jdbcTemplate
        .query(SELECT_BY_VRN_QUERY,
            preparedStatement -> preparedStatement.setString(1, vrn),
            ROW_MAPPER
        );
    if (results.size() > 1) {
      throw new ApplicationRuntimeException(
          String.format("Not able to find unique WhitelistVehicle for vrn: %s", vrn));
    }
    if (results.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(results.iterator().next());
  }

  /**
   * Deletes {@link List} of entities {@link WhitelistVehicle}.
   *
   * @param vrns {@link List} which will identify {@link WhitelistVehicle}.
   */
  public void deleteByVrnsIn(Collection<String> vrns) {
    Preconditions.checkArgument(!vrns.isEmpty(), "VRNs cannot be empty");
    SqlParameterSource namedParameters = new MapSqlParameterSource("vrns", vrns);

    int numberOfDeletedItems = namedParameterJdbcTemplate.update(DELETE_SQL, namedParameters);
    log.info("Deleted {} elements from t_whitelist_vehicles table", numberOfDeletedItems);
  }

  /**
   * Finds all whitelisted vehicles from {@code t_whitelist_vehicles} table.
   */
  public List<WhitelistVehicle> findAll() {
    List<WhitelistVehicle> vehicles = jdbcTemplate.query(SELECT_ALL_SQL, ROW_MAPPER);
    log.info("Found {} vehicles in t_whitelist_vehicles table", vehicles.size());
    return vehicles;
  }

  @VisibleForTesting
  static class WhitelistBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<WhitelistVehicle> batch;

    WhitelistBatchPreparedStatementSetter(List<WhitelistVehicle> batch) {
      this.batch = batch;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
      WhitelistVehicle whitelistVehicle = batch.get(index);
      setInsertStatementAttributes(preparedStatement, whitelistVehicle);
    }

    @Override
    public int getBatchSize() {
      return batch.size();
    }

    private int setInsertStatementAttributes(PreparedStatement preparedStatement,
        WhitelistVehicle whitelistVehicle) throws SQLException {
      int i = 0;
      preparedStatement.setString(++i, whitelistVehicle.getReasonUpdated());
      preparedStatement.setString(++i, whitelistVehicle.getManufacturer().orElse(null));
      preparedStatement.setObject(++i, whitelistVehicle.getUploaderId());
      preparedStatement.setObject(++i, whitelistVehicle.getUploaderEmail());
      preparedStatement.setString(++i, whitelistVehicle.getCategory());
      preparedStatement.setBoolean(++i, whitelistVehicle.isExempt());
      preparedStatement.setBoolean(++i, whitelistVehicle.isCompliant());
      preparedStatement.setString(++i, whitelistVehicle.getVrn());
      return i;
    }
  }

  static class WhitelistVehicleRowMapper implements RowMapper<WhitelistVehicle> {

    @Override
    public WhitelistVehicle mapRow(ResultSet resultSet, int i) throws SQLException {
      return WhitelistVehicle.builder()
          .vrn(resultSet.getString("vrn"))
          .manufacturer(resultSet.getString("manufacturer"))
          .reasonUpdated(resultSet.getString("reason_updated"))
          .updateTimestamp(resultSet.getObject("update_timestamp", LocalDateTime.class))
          .insertTimestamp(resultSet.getObject("insert_timestamp", LocalDateTime.class))
          .uploaderId(UUID.fromString(resultSet.getString("uploader_id")))
          .uploaderEmail(resultSet.getString("uploader_email"))
          .category(resultSet.getString("category"))
          .exempt(resultSet.getBoolean("exempt"))
          .compliant(resultSet.getBoolean("compliant"))
          .build();
    }
  }
}