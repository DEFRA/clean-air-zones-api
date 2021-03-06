package uk.gov.caz.taxiregister.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.RegisterJobInfoRepository;
import uk.gov.caz.taxiregister.repository.RegisterJobRepository;

/**
 * This class acts as a supervisor around running Register Jobs. It manages status and database
 * updates of any job that will be passed to it by implementation of {@link RegisterJobInvoker}
 * interface.
 */
@Slf4j
@Service
@AllArgsConstructor
public class RegisterJobSupervisor {

  @FunctionalInterface
  public interface RegisterJobInvoker {

    void invoke(int registerJobId);
  }

  /**
   * Parameters required to start register job.
   */
  @Value
  @Builder
  public static class StartParams {

    /**
     * Informs supervisor of the source the job is originating from.
     */
    RegisterJobTrigger registerJobTrigger;

    /**
     * Allows to add our own suffix to job name generated by supervisor.
     */
    String registerJobNameSuffix;

    /**
     * CorrelationID to track the request from the API gateway through the Enquiries stack.
     */
    String correlationId;

    /**
     * Identifies user who started register job.
     */
    UUID uploaderId;

    /**
     * Implementation of {@link RegisterJobInvoker} that will start register job.
     */
    RegisterJobInvoker registerJobInvoker;
  }

  private final LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  private final RegisterJobRepository registerJobRepository;

  private final RegisterJobInfoRepository registerJobInfoRepository;

  private final RegisterJobNameGenerator registerJobNameGenerator;

  /**
   * Starts Register Job specified in passed implementation of {@link RegisterJobInvoker} interface
   * and supervises it. It will create proper Register Job database entries and provide name as a
   * handle for future interactions with supervisor.
   *
   * @param params {@link StartParams} with parameters required to start register job.
   * @return Name of started Register Job. Clients can use to to poll or query for status of
   *     Register Job. Useful for debugging and logging purposes as well.
   */
  public RegisterJobName start(StartParams params) {
    RegisterJobName jobName = registerJobNameGenerator
        .generate(params.registerJobNameSuffix, params.registerJobTrigger);
    RegisterJob registerJob = createNewRegisterJob(params.registerJobTrigger, params.correlationId,
        jobName, params.uploaderId);

    int registerJobId = registerJobRepository.insert(registerJob);
    registerJob.setId(registerJobId);

    log.info("About to invoke register job with id '{}' and name '{}'", registerJobId,
        jobName.getValue());
    params.registerJobInvoker.invoke(registerJobId);
    return jobName;
  }

  /**
   * Check if there is already RUNNING or STARTING JOB that attempts to update.
   * given licensing authorities
   * 
   * @param licensingAuthorities A set of {@link LicensingAuthority}.
   * @return true if already running or starting job false otherwise.
   */
  public boolean hasActiveJobs(Set<LicensingAuthority> licensingAuthorities) {
    Integer count;
    count = registerJobRepository.countActiveJobsBy(licensingAuthorities);
    return count != null && count > 0;
  }
  
  /**
   * Creates a new instance of {@link RegisterJob} based on passed parameters.
   */
  private RegisterJob createNewRegisterJob(RegisterJobTrigger registerJobTrigger,
      String correlationId, RegisterJobName jobName, UUID uploaderId) {
    return RegisterJob.builder()
        .trigger(registerJobTrigger)
        .uploaderId(uploaderId)
        .status(RegisterJobStatus.STARTING)
        .jobName(jobName)
        .correlationId(correlationId)
        .build();
  }

  /**
   * Will try to find {@link RegisterJob} with name passed in parameter.
   *
   * @param registerJobName Name of {@link RegisterJob} that will be fetched.
   * @return {@link Optional} of {@link RegisterJob} - if RegisterJob with specified name exists it
   *     will returned in the Optional otherwise it will return empty one.
   */
  public Optional<RegisterJob> findJobWithName(RegisterJobName registerJobName) {
    return registerJobRepository.findByName(registerJobName.getValue());
  }
  
  /**
   * Will try to find {@link RegisterJob} with Id passed in parameter.
   *
   * @param registerJobId Id of {@link RegisterJob} that will be fetched.
   * @return {@link Optional} of {@link RegisterJob} - if RegisterJob with specified Id exists it
   *     will returned in the Optional otherwise it will return empty one.
   */
  public Optional<RegisterJob> findJobById(int registerJobId) {
    return registerJobRepository.findById(registerJobId);
  }

  /**
   * Transactionally sets {@link RegisterJobStatus#FINISHED_SUCCESS} status of an existing job and
   * inserts data to {@code T_MD_REGISTER_JOBS_INFO} table.
   *
   * @param registerJobId An id of the job which is to be updated.
   * @param affectedLicensingAuthorities A set of {@link LicensingAuthority} whose IDs will be
   *     inserted into {@code T_MD_REGISTER_JOBS_INFO} table.
   */
  @Transactional
  public void markSuccessfullyFinished(int registerJobId,
      Set<LicensingAuthority> affectedLicensingAuthorities) {
    registerJobRepository.updateStatus(registerJobId, RegisterJobStatus.FINISHED_SUCCESS);
    registerJobInfoRepository.insert(registerJobId, affectedLicensingAuthorities);
  }

  /**
   * Updates status of existing job.
   *
   * @param registerJobId ID of register job.
   * @param newStatus New status to set.
   */
  public void updateStatus(int registerJobId, RegisterJobStatus newStatus) {
    registerJobRepository.updateStatus(registerJobId, newStatus);
  }

  /**
   * Transactionally updates status and errors of an existing job.
   *
   * @param registerJobId ID of the register job.
   * @param jobStatus Status which will be set against the job.
   * @param validationErrors Validation errors whose details need to be saved in the database
   */
  @Transactional
  public void markFailureWithValidationErrors(int registerJobId, RegisterJobStatus jobStatus,
      List<ValidationError> validationErrors) {
    updateStatus(registerJobId, jobStatus);
    addErrors(registerJobId, validationErrors);
  }

  /**
   * Updates errors of existing job.
   *
   * @param registerJobId ID of register job.
   * @param validationErrors Validation errors whose details need to be saved in the database
   */
  private void addErrors(int registerJobId, List<ValidationError> validationErrors) {
    List<RegisterJobError> errors = validationErrors.stream()
        .sorted()
        .map(RegisterJobError::from)
        .collect(Collectors.toList());
    registerJobRepository.updateErrors(registerJobId, errors);
  }
  
  /**
   * Lock given set of Licensing Authorities to prevent other jobs.
   * from modifying their data
   *    
   * @param registerJobId ID of the register job.
   * @param licensingAuthorities a set of Licensing Authorities
   */
  public void lockImpactedLocalAuthorities(int registerJobId,
      Set<LicensingAuthority> licensingAuthorities) {
    registerJobRepository.updateImpactedLocalAuthority(registerJobId, licensingAuthorities);
  }
}
