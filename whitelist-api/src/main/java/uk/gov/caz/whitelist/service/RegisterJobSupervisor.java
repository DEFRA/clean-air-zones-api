package uk.gov.caz.whitelist.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.whitelist.model.CsvContentType;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.registerjob.RegisterJob;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobError;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobName;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobStatus;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.whitelist.repository.RegisterJobRepository;

/**
 * This class acts as a supervisor around running Register Jobs. It manages status and database
 * updates of any job that will be passed to it by implementation of {@link RegisterJobInvoker}
 * interface.
 */
@Service
@Slf4j
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

  private final RegisterJobRepository registerJobRepository;

  private final RegisterJobNameGenerator registerJobNameGenerator;

  public RegisterJobSupervisor(RegisterJobRepository registerJobRepository,
      RegisterJobNameGenerator registerJobNameGenerator) {
    this.registerJobRepository = registerJobRepository;
    this.registerJobNameGenerator = registerJobNameGenerator;
  }

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

  public boolean hasActiveJobsFor(CsvContentType csvContentType) {
    Integer count = registerJobRepository.countActiveJobsByContentType(csvContentType);
    return count != null && count > 0;
  }

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
   * Updates status of existing job.
   *
   * @param registerJobId ID of register job.
   * @param newStatus New status to set.
   */
  public void updateStatus(int registerJobId, RegisterJobStatus newStatus) {
    registerJobRepository.updateStatus(registerJobId, newStatus);
  }

  /**
   * Updates errors of existing job.
   *
   * @param registerJobId ID of register job.
   * @param validationErrors Validation errors whose details need to be saved in the database
   */
  public void addErrors(int registerJobId, List<ValidationError> validationErrors) {
    List<RegisterJobError> errors = validationErrors.stream()
        .map(RegisterJobError::from)
        .collect(Collectors.toList());
    registerJobRepository.updateErrors(registerJobId, errors);
  }

  /**
   * Updates status and errors of an existing job.
   *
   * @param registerJobId ID of the register job.
   * @param jobStatus Status which will be set against the job.
   * @param validationErrors Validation errors whose details need to be saved in the database
   */
  public void markFailureWithValidationErrors(int registerJobId, RegisterJobStatus jobStatus,
      List<ValidationError> validationErrors) {
    updateStatus(registerJobId, jobStatus);
    addErrors(registerJobId, validationErrors);
  }
}
