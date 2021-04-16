package uk.gov.caz.taxiregister.repository;

import static java.util.stream.Collectors.groupingBy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;

/**
 * A class that is responsible for managing vehicle's licences data ({@link TaxiPhvVehicleLicence}
 * entities) in the postgres database.
 */
@Slf4j
@Repository
public class TaxiPhvLicencePostgresRepository {

  protected static final ImmutableMap<String, Boolean> EXPECTED_BOOLEAN_VALUES =
      new ImmutableMap.Builder<String, Boolean>()
          .put("y", Boolean.TRUE)
          .put("n", Boolean.FALSE)
          .build();

  public static final String SELECT_FROM_TAXI_PHV_AND_LICENSING_AUTHORITY =
      "SELECT taxi_phv.taxi_phv_register_id, "
          + "taxi_phv.vrm, "
          + "taxi_phv.description, "
          + "taxi_phv.licence_start_date, "
          + "taxi_phv.licence_end_date, "
          + "taxi_phv.licence_authority_id, "
          + "taxi_phv.licence_plate_number, "
          + "taxi_phv.wheelchair_access_flag, "
          + "taxi_phv.uploader_id, "
          + "taxi_phv.insert_timestmp, "
          + "la.licence_authority_name "
          + "FROM t_md_taxi_phv taxi_phv, t_md_licensing_authority la ";

  @VisibleForTesting
  static final String SELECT_ALL_SQL = SELECT_FROM_TAXI_PHV_AND_LICENSING_AUTHORITY
      + "WHERE la.licence_authority_id = taxi_phv.licence_authority_id";

  @VisibleForTesting
  static final String SELECT_BY_LICENSING_AUTHORITY_ID_SQL =
      SELECT_FROM_TAXI_PHV_AND_LICENSING_AUTHORITY
          + "WHERE la.licence_authority_id = taxi_phv.licence_authority_id "
          + "AND la.licence_authority_id = ?";

  @VisibleForTesting
  static final String SELECT_BY_VRM_SQL = SELECT_FROM_TAXI_PHV_AND_LICENSING_AUTHORITY
      + "WHERE la.licence_authority_id = taxi_phv.licence_authority_id "
      + "AND taxi_phv.vrm = ?";

  static final String SELECT_BY_VRMS_SQL = SELECT_FROM_TAXI_PHV_AND_LICENSING_AUTHORITY
      + "WHERE la.licence_authority_id = taxi_phv.licence_authority_id "
      + "AND taxi_phv.vrm in (:vrms)";

  @VisibleForTesting
  static final String INSERT_SQL = "INSERT INTO t_md_taxi_phv("
      + "vrm, "
      + "description, "
      + "licence_start_date, "
      + "licence_end_date, "
      + "licence_authority_id, "
      + "licence_plate_number, "
      + "wheelchair_access_flag, "
      + "uploader_id, "
      + "insert_timestmp) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

  @VisibleForTesting
  static final String UPDATE_SQL = "UPDATE t_md_taxi_phv "
      + "SET "
      + "vrm = ?, "
      + "description = ?, "
      + "licence_start_date = ?, "
      + "licence_end_date = ?, "
      + "licence_authority_id = ?, "
      + "licence_plate_number = ?, "
      + "wheelchair_access_flag = ?, "
      + "uploader_id = ? "
      + "WHERE taxi_phv_register_id = ?";

  @VisibleForTesting
  static final String DELETE_SQL = "DELETE FROM "
      + "t_md_taxi_phv "
      + "WHERE taxi_phv_register_id = ?";

  private static final String DELETE_ALL_SQL = "DELETE FROM "
      + "t_md_taxi_phv";

  private static final LicenceRowMapper MAPPER = new LicenceRowMapper();

  private final int updateBatchSize;
  private final int deleteBatchSize;
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  /**
   * Creates an instance of {@link TaxiPhvLicencePostgresRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}.
   * @param updateBatchSize The number of records that should be inserted or updated in one
   *     batch database operation.
   * @param deleteBatchSize The number of records that should be deleted in one batch database
   */
  public TaxiPhvLicencePostgresRepository(JdbcTemplate jdbcTemplate,
      @Value("${application.jdbc.updateBatchSize:100}") int updateBatchSize,
      @Value("${application.jdbc.deleteBatchSize:100}") int deleteBatchSize,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.updateBatchSize = updateBatchSize;
    this.deleteBatchSize = deleteBatchSize;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
  }

  /**
   * Returns all licences for a given {@code licensingAuthorityId}.
   *
   * @param licensingAuthorityId An id of the licensing authority.
   * @return {@link Set} containing all {@link TaxiPhvVehicleLicence} entities in the database for a
   *     given licensing authority.
   */
  public Set<TaxiPhvVehicleLicence> findByLicensingAuthority(int licensingAuthorityId) {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = jdbcTemplate.query(
        SELECT_BY_LICENSING_AUTHORITY_ID_SQL,
        preparedStatement -> preparedStatement.setObject(1, licensingAuthorityId),
        MAPPER
    );
    return new HashSet<>(taxiPhvVehicleLicences);
  }

  /**
   * Updates the corresponding licences in the database.
   *
   * @param taxiPhvVehicleLicences A set of {@link TaxiPhvVehicleLicence} which needs to be
   *     updated
   */
  public void update(Set<TaxiPhvVehicleLicence> taxiPhvVehicleLicences) {
    Iterable<List<TaxiPhvVehicleLicence>> batches = Iterables
        .partition(taxiPhvVehicleLicences, updateBatchSize);
    for (List<TaxiPhvVehicleLicence> batch : batches) {
      jdbcTemplate.batchUpdate(UPDATE_SQL, new UpdateBatchPreparedStatementSetter(batch));
    }
  }

  /**
   * Inserts passed {@code taxiPhvVehicleLicences} in the database.
   *
   * @param taxiPhvVehicleLicences A set of {@link TaxiPhvVehicleLicence} that needs to be
   *     inserted
   */
  public void insert(Set<TaxiPhvVehicleLicence> taxiPhvVehicleLicences) {
    Iterable<List<TaxiPhvVehicleLicence>> batches = Iterables
        .partition(taxiPhvVehicleLicences, updateBatchSize);
    for (List<TaxiPhvVehicleLicence> batch : batches) {
      jdbcTemplate.batchUpdate(INSERT_SQL, new InsertBatchPreparedStatementSetter(batch));
    }
  }

  /**
   * Deletes passed {@code licencesIds} from the database.
   *
   * @param licencesIds A set of {@link TaxiPhvVehicleLicence} ids that needs to be deleted.
   */
  public void delete(Set<Integer> licencesIds) {
    List<List<Integer>> batches = Lists.partition(Lists.newArrayList(licencesIds),
        deleteBatchSize);
    for (List<Integer> batch : batches) {
      jdbcTemplate.batchUpdate(DELETE_SQL, new DeleteBatchPreparedStatementSetter(batch));
    }
  }

  /**
   * Deletes ALL licences from the database.
   */
  public void deleteAll() {
    jdbcTemplate.update(DELETE_ALL_SQL);
  }

  /**
   * Finds all {@link TaxiPhvVehicleLicence} entities in the database.
   *
   * @return a {@link List} of all {@link TaxiPhvVehicleLicence} entities in the database
   */
  public List<TaxiPhvVehicleLicence> findAll() {
    return jdbcTemplate.query(SELECT_ALL_SQL, MAPPER);
  }

  /**
   * Finds all {@link TaxiPhvVehicleLicence} entities for a given vrm.
   *
   * @param vrm for which all matching licences are returned
   * @return {@link List} of all {@link TaxiPhvVehicleLicence} which matches passed vrm.
   */
  public List<TaxiPhvVehicleLicence> findByVrm(String vrm) {
    return jdbcTemplate.query(
        SELECT_BY_VRM_SQL,
        preparedStatement -> preparedStatement.setString(1, vrm),
        MAPPER
    );
  }

  /**
   * Finds all {@link TaxiPhvVehicleLicence} entities for given vrms. The returned result is
   * grouped by vrm.
   */
  public Map<String, List<TaxiPhvVehicleLicence>> findByVrms(Set<String> vrms) {
    if (vrms.isEmpty()) {
      return Collections.emptyMap();
    }
    SqlParameterSource namedParameters = new MapSqlParameterSource("vrms", vrms);
    List<TaxiPhvVehicleLicence> licences = namedParameterJdbcTemplate
        .query(SELECT_BY_VRMS_SQL, namedParameters, MAPPER);
    return licences.stream()
        .collect(groupingBy(TaxiPhvVehicleLicence::getVrm));
  }

  @VisibleForTesting
  static class LicenceRowMapper implements RowMapper<TaxiPhvVehicleLicence> {

    @Override
    public TaxiPhvVehicleLicence mapRow(ResultSet rs, int i) throws SQLException {
      return TaxiPhvVehicleLicence.builder()
          .id(rs.getInt("taxi_phv_register_id"))
          .uploaderId(rs.getObject("uploader_id", UUID.class))
          .vrm(rs.getString("vrm"))
          .description(rs.getString("description"))
          .licenseDates(
              new LicenseDates(
                  rs.getObject("licence_start_date", LocalDate.class),
                  rs.getObject("licence_end_date", LocalDate.class)))
          .licensingAuthority(new LicensingAuthority(
              rs.getInt("licence_authority_id"),
              rs.getString("licence_authority_name")
          ))
          .licensePlateNumber(rs.getString("licence_plate_number"))
          .wheelchairAccessible(mapWheelchairAccessFlag(rs.getString("wheelchair_access_flag")))
          .addedTimestamp(rs.getObject("insert_timestmp", LocalDateTime.class))
          .build();
    }

    static Boolean mapWheelchairAccessFlag(String wheelchairAccessFlag) {
      return EXPECTED_BOOLEAN_VALUES.getOrDefault(wheelchairAccessFlag, null);
    }
  }

  @VisibleForTesting
  static class InsertBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<TaxiPhvVehicleLicence> batch;

    InsertBatchPreparedStatementSetter(List<TaxiPhvVehicleLicence> batch) {
      this.batch = batch;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
      TaxiPhvVehicleLicence taxiPhvVehicleLicence = batch.get(index);
      setCommonInsertUpdateStatementAttributes(preparedStatement, taxiPhvVehicleLicence);
    }

    @Override
    public int getBatchSize() {
      return batch.size();
    }
  }

  @VisibleForTesting
  static class UpdateBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<TaxiPhvVehicleLicence> batch;

    UpdateBatchPreparedStatementSetter(List<TaxiPhvVehicleLicence> batch) {
      this.batch = batch;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
      TaxiPhvVehicleLicence taxiPhvVehicleLicence = batch.get(index);
      int i = setCommonInsertUpdateStatementAttributes(preparedStatement, taxiPhvVehicleLicence);
      preparedStatement.setObject(++i, taxiPhvVehicleLicence.getId());
    }

    @Override
    public int getBatchSize() {
      return batch.size();
    }
  }

  @VisibleForTesting
  static class DeleteBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<Integer> batch;

    DeleteBatchPreparedStatementSetter(List<Integer> batch) {
      this.batch = batch;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
      int id = batch.get(index);
      preparedStatement.setInt(1, id);
    }

    @Override
    public int getBatchSize() {
      return batch.size();
    }
  }

  private static int setCommonInsertUpdateStatementAttributes(PreparedStatement preparedStatement,
      TaxiPhvVehicleLicence licence) throws SQLException {
    int i = 0;
    preparedStatement.setString(++i, licence.getVrm());
    preparedStatement.setString(++i, licence.getDescription());
    preparedStatement.setObject(++i, licence.getLicenseDates().getStart());
    preparedStatement.setObject(++i, licence.getLicenseDates().getEnd());
    preparedStatement.setInt(++i, licence.getLicensingAuthority().getId());
    preparedStatement.setString(++i, licence.getLicensePlateNumber());
    preparedStatement.setString(++i, Optional.ofNullable(licence.getWheelchairAccessible())
        .map(wheelchair -> wheelchair ? "y" : "n")
        .orElse(null));
    preparedStatement.setObject(++i, licence.getUploaderId());
    return i;
  }

  /**
   * Helper method for evicting a cached vehicles from redis.
   */
  @CacheEvict(value = "vehicles", allEntries = true)
  public void cacheEvictLicensingRepository() {
    log.debug(
        "Evicting cached VRNs in response to a data upload being made by a third party");
  }
}