package uk.gov.caz.whitelist;

import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_ERRORS;
import static uk.gov.caz.testutils.TestObjects.NOT_EXISTING_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_WHITELIST_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.S3_RUNNING_REGISTER_JOB;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_ERRORS;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_RUNNING_REGISTER_JOB_STATUS;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.whitelist.annotation.IntegrationTest;
import uk.gov.caz.whitelist.model.CsvContentType;
import uk.gov.caz.whitelist.model.registerjob.RegisterJob;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobError;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobStatus;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.whitelist.repository.RegisterJobRepository;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class RegisterJobRepositoryTestIT {

  @Autowired
  private RegisterJobRepository registerJobRepository;

  @Test
  public void testRegisterJobRepositoryOperations() {
    // Insert
    int autoGeneratedId = insertNewRegisterJob();

    // Find by id
    queryForNewlyInsertedRegisterJob(autoGeneratedId);

    // Find the same job by name
    RegisterJob fetchedRegisterJob = queryForNewlyInsertedRegisterJob(S3_REGISTER_JOB_NAME,
        autoGeneratedId);

    // Update status on entity
    updateStatus(fetchedRegisterJob, RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS);
    queryForUpdatedRegisterJob(autoGeneratedId,
        RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, TYPICAL_REGISTER_JOB_ERRORS);

    // Update status directly on database
    updateStatus(autoGeneratedId, RegisterJobStatus.FINISHED_SUCCESS);
    queryForUpdatedRegisterJob(autoGeneratedId, RegisterJobStatus.FINISHED_SUCCESS,
        TYPICAL_REGISTER_JOB_ERRORS);

    // Update errors directly on database
    updateErrors(autoGeneratedId, MODIFIED_REGISTER_JOB_ERRORS);
    queryForUpdatedRegisterJob(autoGeneratedId, RegisterJobStatus.FINISHED_SUCCESS,
        MODIFIED_REGISTER_JOB_ERRORS);

    // Find by non existing id
    queryForNonExistingRegisterJobById();

    // Find by non existing job name
    queryForNonExistingRegisterJobByName();
  }

  @Test
  @Sql("classpath:data/sql/register-job-data.sql")
  public void shouldReturnNumberOfActivatedJobs() {
    // given

    // when
    Integer result = registerJobRepository
        .countActiveJobsByContentType(CsvContentType.WHITELIST_LIST);

    // then
    assertThat(result).isEqualTo(2);
  }

  @Test
  public void shouldReturnNotActivatedJobs() {
    // given

    // when
    Integer result = registerJobRepository
        .countActiveJobsByContentType(CsvContentType.WHITELIST_LIST);

    // then
    assertThat(result).isEqualTo(0);
  }

  private int insertNewRegisterJob() {
    RegisterJob registerJob = S3_RUNNING_REGISTER_JOB;
    int autoGeneratedId = registerJobRepository.insert(registerJob);
    registerJob.setId(autoGeneratedId);
    checkIfAutoGeneratedIdWasBumpedAndInsertedIntoRegisterJobEntity(registerJob, autoGeneratedId);
    return autoGeneratedId;
  }

  private void checkIfAutoGeneratedIdWasBumpedAndInsertedIntoRegisterJobEntity(
      RegisterJob registerJob, int autoGeneratedId) {
    assertThat(autoGeneratedId).isGreaterThan(0);
    assertThat(registerJob.getId()).isEqualTo(autoGeneratedId);
  }

  private RegisterJob queryForNewlyInsertedRegisterJob(int autoGeneratedId) {
    RegisterJob fetchedRegisterJob = findByIdAndValidateThatItExists(autoGeneratedId);
    validateThatHasProperties(fetchedRegisterJob, autoGeneratedId,
        S3_WHITELIST_REGISTER_JOB_TRIGGER,
        S3_REGISTER_JOB_NAME, TYPICAL_REGISTER_JOB_UPLOADER_ID,
        TYPICAL_RUNNING_REGISTER_JOB_STATUS,
        TYPICAL_REGISTER_JOB_ERRORS, TYPICAL_CORRELATION_ID);
    return fetchedRegisterJob;
  }

  private RegisterJob findByIdAndValidateThatItExists(int id) {
    Optional<RegisterJob> fetchedRegisterJobOptional = registerJobRepository.findById(id);
    assertThat(fetchedRegisterJobOptional).isPresent();
    return fetchedRegisterJobOptional.get();
  }

  private RegisterJob queryForNewlyInsertedRegisterJob(String jobName, int autoGeneratedId) {
    RegisterJob fetchedRegisterJob = findByNameAndValidateThatItExists(jobName);
    validateThatHasProperties(fetchedRegisterJob, autoGeneratedId,
        S3_WHITELIST_REGISTER_JOB_TRIGGER,
        S3_REGISTER_JOB_NAME, TYPICAL_REGISTER_JOB_UPLOADER_ID,
        TYPICAL_RUNNING_REGISTER_JOB_STATUS,
        TYPICAL_REGISTER_JOB_ERRORS, TYPICAL_CORRELATION_ID);
    return fetchedRegisterJob;
  }

  private RegisterJob findByNameAndValidateThatItExists(String jobName) {
    Optional<RegisterJob> fetchedRegisterJobOptional = registerJobRepository.findByName(jobName);
    assertThat(fetchedRegisterJobOptional).isPresent();
    return fetchedRegisterJobOptional.get();
  }

  private RegisterJob updateStatus(RegisterJob registerJob, RegisterJobStatus newStatus) {
    RegisterJob updatedRegisterJob = registerJob.toBuilder().status(newStatus).build();
    registerJobRepository.updateStatus(updatedRegisterJob);
    return updatedRegisterJob;
  }

  private void queryForUpdatedRegisterJob(int autoGeneratedId, RegisterJobStatus expectedStatus,
      List<RegisterJobError> expectedErrors) {
    RegisterJob fetchedRegisterJob = findByIdAndValidateThatItExists(autoGeneratedId);
    validateThatHasProperties(fetchedRegisterJob, autoGeneratedId,
        S3_WHITELIST_REGISTER_JOB_TRIGGER,
        S3_REGISTER_JOB_NAME, TYPICAL_REGISTER_JOB_UPLOADER_ID,
        expectedStatus, expectedErrors, TYPICAL_CORRELATION_ID);
  }

  private void updateStatus(int autoGeneratedId, RegisterJobStatus newStatus) {
    registerJobRepository.updateStatus(autoGeneratedId, newStatus);
  }

  private void validateThatHasProperties(RegisterJob registerJob, int expectedId,
      RegisterJobTrigger expectedJobTrigger, String expectedJobName, UUID expectedUploaderId,
      RegisterJobStatus expectedJobStatus, List<RegisterJobError> expectedErrors,
      String expectedCorrelationId) {
    assertThat(registerJob)
        .hasId(expectedId)
        .wasTriggeredBy(expectedJobTrigger)
        .hasName(expectedJobName)
        .wasUploadedBy(expectedUploaderId)
        .isInStatus(expectedJobStatus)
        .hasErrors(expectedErrors)
        .hasCorrelationId(expectedCorrelationId);
  }

  private void updateErrors(int autoGeneratedId, List<RegisterJobError> newErrors) {
    registerJobRepository.updateErrors(autoGeneratedId, newErrors);
  }

  private void queryForNonExistingRegisterJobById() {
    Optional<RegisterJob> fetchedRegisterJobOptional = registerJobRepository.findById(99887766);
    assertThat(fetchedRegisterJobOptional).isEmpty();
  }

  private void queryForNonExistingRegisterJobByName() {
    Optional<RegisterJob> fetchedRegisterJobOptional = registerJobRepository
        .findByName(NOT_EXISTING_REGISTER_JOB_NAME);
    assertThat(fetchedRegisterJobOptional).isEmpty();
  }
}
