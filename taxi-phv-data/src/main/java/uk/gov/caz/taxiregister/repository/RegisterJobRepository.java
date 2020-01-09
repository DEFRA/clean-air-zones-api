package uk.gov.caz.taxiregister.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.exception.JobNameDuplicateException;
import uk.gov.caz.taxiregister.util.JsonHelpers;

@Repository
@Slf4j
public class RegisterJobRepository {

  public static final String COL_REGISTER_JOB_ID = "register_job_id";
  public static final String COL_TRIGGER = "trigger";
  public static final String COL_JOB_NAME = "job_name";
  public static final String COL_UPLOADER_ID = "uploader_id";
  public static final String COL_STATUS = "status";
  public static final String COL_ERRORS = "errors";
  public static final String COL_CORRELATION_ID = "correlation_id";
  public static final String COL_IMPACTED_LOCAL_AUTHORITY = "impacted_local_authority";

  private static String selectAllColumns() {
    return "SELECT rj." + COL_REGISTER_JOB_ID + ", "
        + "rj." + COL_TRIGGER + ", "
        + "rj." + COL_JOB_NAME + ", "
        + "rj." + COL_UPLOADER_ID + ", "
        + "rj." + COL_STATUS + ", "
        + "rj." + COL_ERRORS + ", "
        + "rj." + COL_CORRELATION_ID + " "
        + "FROM t_md_register_jobs rj ";
  }

  private static final String SELECT_BY_REGISTER_JOB_ID =
      selectAllColumns() + "WHERE rj." + COL_REGISTER_JOB_ID + " = ?";

  private static final String SELECT_BY_REGISTER_JOB_NAME =
      selectAllColumns() + "WHERE rj." + COL_JOB_NAME + " = ?";

  private static final String SELECT_COUNT_OF_JOBS_THAT_LOCK_GIVEN_LICENSING_AUTHORITY =
      "SELECT count(*) FROM t_md_register_jobs rj "
      + "WHERE rj.impacted_local_authority is not null "
      + "AND (select ARRAY(select licence_authority_id from t_md_licensing_authority "
      + "where licence_authority_name = ANY(?)) "
      + "&& rj.impacted_local_authority) "
      + "AND (" + COL_STATUS + " = \'" + RegisterJobStatus.STARTING
      + "\' OR " + COL_STATUS + " = \'" + RegisterJobStatus.RUNNING + "\')";
  
  
  private static final String UPDATE_STATUS_SQL = "UPDATE t_md_register_jobs "
      + "SET "
      + COL_STATUS + " = ?, "
      + "last_modified_timestmp = CURRENT_TIMESTAMP "
      + "WHERE " + COL_REGISTER_JOB_ID + " = ?";

  private static final String UPDATE_ERRORS_SQL = "UPDATE t_md_register_jobs "
      + "SET "
      + COL_ERRORS + " = ?, "
      + "last_modified_timestmp = CURRENT_TIMESTAMP "
      + "WHERE " + COL_REGISTER_JOB_ID + " = ?";
  
  private static final String LOCK_IMPACTED_LOCAL_AUTHORITY_SQL = "UPDATE t_md_register_jobs "
      + "SET "
      + COL_IMPACTED_LOCAL_AUTHORITY + " = (select ARRAY(select licence_authority_id "
          + "from t_md_licensing_authority "
          + "where licence_authority_name = ANY(?))) "
      + "WHERE " + COL_REGISTER_JOB_ID + " = ?";  
  
  private final RegisterJobRowMapper rowMapper;
  private final JdbcTemplate jdbcTemplate;
  private final SimpleJdbcInsert jdbcInsert;
  private final JsonHelpers jsonHelpers;
  private final int maxErrorsCount;

  /**
   * Creates an instance of {@link RegisterJobRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}.
   * @param jsonHelpers An instance of {@link JsonHelpers}.
   * @param objectMapper An instance of {@link ObjectMapper} used for errors (de)serialization.
   * @param maxErrorsCount The maximum number of errors that can be stored for a single job.
   */
  public RegisterJobRepository(JdbcTemplate jdbcTemplate,
      JsonHelpers jsonHelpers,
      ObjectMapper objectMapper,
      @Value("${registerjob.db.max-errors-count}") int maxErrorsCount) {
    this.jdbcTemplate = jdbcTemplate;
    jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
        .withTableName("t_md_register_jobs")
        .usingGeneratedKeyColumns(COL_REGISTER_JOB_ID)
        .usingColumns(COL_TRIGGER, COL_JOB_NAME, COL_UPLOADER_ID, COL_STATUS, COL_ERRORS,
            COL_CORRELATION_ID);
    this.jsonHelpers = jsonHelpers;
    this.rowMapper = new RegisterJobRowMapper(objectMapper);
    this.maxErrorsCount = maxErrorsCount;
  }

  /**
   * Finds {@link RegisterJob} by its ID.
   *
   * @param id ID of job that will be fetched.
   * @return An {@link Optional} of {@link RegisterJob} with specified ID or without any value if
   *     there is no RegisterJob with such ID.
   */
  public Optional<RegisterJob> findById(int id) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(SELECT_BY_REGISTER_JOB_ID, rowMapper, id));
    } catch (EmptyResultDataAccessException exc) {
      return Optional.empty();
    }
  }

  /**
   * Finds {@link RegisterJob} by its name.
   *
   * @param jobName Name of {@link RegisterJob} that will be fetched.
   * @return An {@link Optional} of {@link RegisterJob} with specified name or without any value if
   *     there is no RegisterJob with such name.
   */
  public Optional<RegisterJob> findByName(String jobName) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(SELECT_BY_REGISTER_JOB_NAME, rowMapper, jobName));
    } catch (EmptyResultDataAccessException exc) {
      return Optional.empty();
    }
  }

  /**
   * Counts active (not finished) jobs that are locking 
   * the given set of {@code licensingAuthorities}.
   * 
   * @param licensingAuthorities A set of {@link LicensingAuthority}.
   * @return An {@link Integer} of active jobs.
   */
  public Integer countActiveJobsBy(Set<LicensingAuthority> licensingAuthorities) {
    List<Integer> result = jdbcTemplate.query(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          SELECT_COUNT_OF_JOBS_THAT_LOCK_GIVEN_LICENSING_AUTHORITY);
      preparedStatement.setArray(1,
          connection.createArrayOf("varchar", toArray(licensingAuthorities)));
      return preparedStatement;
    }, (resultSet, i) -> resultSet.getInt("count"));
    return result.iterator().next();
  }
  
  /**
   * Maps a set of {@link LicensingAuthority} to an array of licensing authorities names.
   */
  private String[] toArray(Set<LicensingAuthority> affectedLicensingAuthorities) {
    return affectedLicensingAuthorities.stream()
        .map(LicensingAuthority::getName)
        .toArray(String[]::new);
  }
  
  /**
   * Inserts passed {@code RegisterJob} in the database.
   *
   * @param registerJob A instance of {@link RegisterJob} that needs to be inserted
   * @return DB auto generated ID
   */
  public int insert(RegisterJob registerJob) {
    Map<String, Object> params = Maps.newHashMap(ImmutableMap.of(
        COL_TRIGGER, registerJob.getTrigger().name(),
        COL_JOB_NAME, registerJob.getJobName().getValue(),
        COL_UPLOADER_ID, registerJob.getUploaderId(),
        COL_STATUS, registerJob.getStatus().name(),
        COL_CORRELATION_ID, registerJob.getCorrelationId()));
    String errorsValue = convertToJsonUnlessEmpty(registerJob.getErrors());
    if (errorsValue != null) {
      params.put(COL_ERRORS, errorsValue);
    }
    Number id = execute(params);
    return (int) id;
  }

  private Number execute(Map<String, Object> params) {
    try {
      return jdbcInsert.executeAndReturnKey(params);
    } catch (DuplicateKeyException e) {
      log.error("There is already job with name {}", params.get(COL_JOB_NAME));
      throw new JobNameDuplicateException("There is already job with given name");
    }
  }

  /**
   * Updates status of existing job. Status is taken from {@link RegisterJob} object.
   *
   * @param registerJob Instance of {@link RegisterJob} which will be used to get new value of
   *     job status.
   */
  public void updateStatus(RegisterJob registerJob) {
    jdbcTemplate.update(UPDATE_STATUS_SQL, registerJob.getStatus().name(), registerJob.getId());
  }

  /**
   * Updates status of existing job.
   *
   * @param registerJobId ID of register job.
   * @param newStatus New status to set.
   */
  public void updateStatus(int registerJobId, RegisterJobStatus newStatus) {
    jdbcTemplate.update(UPDATE_STATUS_SQL, newStatus.name(), registerJobId);
  }

  /**
   * Updates errors of existing job.
   *
   * @param registerJobId ID of register job.
   * @param errorsList New value of errors to set.
   */
  public void updateErrors(int registerJobId, List<RegisterJobError> errorsList) {
    Preconditions.checkNotNull(errorsList);
    String errors = convertToJsonUnlessEmpty(truncate(errorsList));
    jdbcTemplate.update(UPDATE_ERRORS_SQL, errors, registerJobId);
  }

  /**
   * Lock local authorities whose data the given job is attempting to update.
   *
   * @param registerJobId ID of register job.
   * @param licensingAuthorities a set of LicensingAuthorities.
   */
  public void updateImpactedLocalAuthority(int registerJobId,
      Set<LicensingAuthority> licensingAuthorities) {
    Preconditions.checkNotNull(licensingAuthorities);

    jdbcTemplate.update(connection -> {
      PreparedStatement preparedStatement = connection.prepareStatement(
          LOCK_IMPACTED_LOCAL_AUTHORITY_SQL);
      preparedStatement.setArray(1, connection.createArrayOf("varchar",
          toArray(licensingAuthorities)));
      preparedStatement.setInt(2, registerJobId);
      return preparedStatement;
    });
  }
  
  private List<RegisterJobError> truncate(List<RegisterJobError> errorsList) {
    if (errorsList.size() > maxErrorsCount) {
      log.warn("Errors list contains too many elements ({}), truncating it to maximum allowed: {}.",
          errorsList.size(), maxErrorsCount);
      return errorsList.subList(0, maxErrorsCount);
    }
    return errorsList;
  }

  /**
   * Returns {@code null} if {@code errors} is empty, a converted object to JSON otherwise.
   */
  private String convertToJsonUnlessEmpty(List<RegisterJobError> errors) {
    return errors.isEmpty() ? null : jsonHelpers.toJson(errors);
  }
  
  @VisibleForTesting
  static class RegisterJobRowMapper implements RowMapper<RegisterJob> {

    private final ObjectMapper objectMapper;

    RegisterJobRowMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public RegisterJob mapRow(ResultSet rs, int i) throws SQLException {
      String errors = rs.getString(COL_ERRORS);
      return RegisterJob.builder()
          .id(rs.getInt(COL_REGISTER_JOB_ID))
          .trigger(RegisterJobTrigger.valueOf(rs.getString(COL_TRIGGER)))
          .jobName(new RegisterJobName(rs.getString(COL_JOB_NAME)))
          .uploaderId(rs.getObject(COL_UPLOADER_ID, UUID.class))
          .status(RegisterJobStatus.valueOf(rs.getString(COL_STATUS)))
          .errors(errors == null ? Collections.emptyList() : convertFromJson(errors))
          .correlationId(rs.getString(COL_CORRELATION_ID))
          .build();
    }

    private List<RegisterJobError> convertFromJson(String input) {
      try {
        return objectMapper.readValue(input, new TypeReference<List<RegisterJobError>>() {
        });
      } catch (IOException e) {
        log.error("Cannot convert list of job errors.", e);
        throw new UncheckedIOException(e);
      }
    }
  }
}
