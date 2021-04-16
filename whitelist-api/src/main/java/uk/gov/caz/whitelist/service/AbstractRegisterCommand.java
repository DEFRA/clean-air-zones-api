package uk.gov.caz.whitelist.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobStatus;

/**
 * Abstract class which is responsible for registering vehicles. It is not aware of underlying
 * vehicle storage (let it be REST API, CSV in S3).
 */
@Slf4j
public abstract class AbstractRegisterCommand {

  private final int registerJobId;
  private final String correlationId;
  private final int maxValidationErrorCount;

  private final RegisterService registerService;
  private final RegisterFromCsvExceptionResolver exceptionResolver;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final WhitelistedVehicleDtoToModelConverter vehiclesConverter;

  /**
   * Creates an instance of {@link AbstractRegisterCommand}.
   */
  public AbstractRegisterCommand(RegisterServicesContext registerServicesContext, 
      int registerJobId,
      String correlationId) {
    this.registerService = registerServicesContext.getRegisterService();
    this.exceptionResolver = registerServicesContext.getExceptionResolver();
    this.registerJobSupervisor = registerServicesContext.getRegisterJobSupervisor();
    this.vehiclesConverter = registerServicesContext.getDtoToModelConverter();
    this.maxValidationErrorCount = registerServicesContext.getMaxValidationErrorCount();
    this.registerJobId = registerJobId;
    this.correlationId = correlationId;
  }

  abstract void beforeExecute();

  abstract List<WhitelistedVehicleDto> getVehiclesToRegister();

  /**
   * Gets {@code uploader-id}.
   *
   * @return {@link UUID} which represents the uploader.
   */
  abstract UUID getUploaderId();

  /**
   * Gets {@code email}.
   *
   * @return {@link String} which represents the uploader email.
   */
  abstract String getUploaderEmail();

  abstract List<ValidationError> getParseValidationErrors();

  /**
   * Returns a boolean indicating whether the job should be marked as failed.
   */
  abstract boolean shouldMarkJobFailed();

  /**
   * Method hook before marking job as failed.
   */
  abstract void onBeforeMarkJobFailed();

  /**
   * Method that executes logic common for all providers eg. S3 or REST API.
   *
   * @return {@link RegisterResult} register result of given rows.
   */
  public final RegisterResult execute() {
    try {
      log.info("Processing registration, correlation-id: '{}' : start", getCorrelationId());

      markJobRunning();

      beforeExecute();

      ConversionResults conversionResults = vehiclesConverter.convert(
          getVehiclesToRegister(), getUploaderId(), getUploaderEmail());

      if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
        List<ValidationError> errors = combineValidationErrors(
            conversionResults.getValidationErrors(),
            getParseValidationErrors()
        );
        markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, errors);
        return RegisterResult.failure(errors);
      }

      RegisterResult result = registerService.register(
          conversionResults,
          getUploaderId(),
          getUploaderEmail()
      );

      postProcessRegistrationResult(result);

      return result;
    } catch (Exception e) {
      RegisterResult result = exceptionResolver.resolve(e);
      markJobFailed(exceptionResolver.resolveToRegisterJobFailureStatus(e),
          result.getValidationErrors());
      return result;
    } finally {
      log.info("Processing registration, correlation-id: '{}' : finish", getCorrelationId());
    }
  }

  private boolean hasParseValidationErrors() {
    return !getParseValidationErrors().isEmpty();
  }

  private List<ValidationError> combineValidationErrors(List<ValidationError> a,
      List<ValidationError> b) {
    return Stream.of(a, b)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparingInt(value -> value.getLineNumber().orElse(0)))
        .limit(maxValidationErrorCount)
        .collect(Collectors.toList());
  }

  private void postProcessRegistrationResult(RegisterResult result) {
    if (result.isSuccess()) {
      markJobFinished();
    } else {
      markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
          result.getValidationErrors());
    }
  }

  private void markJobFinished() {
    registerJobSupervisor.updateStatus(getRegisterJobId(), RegisterJobStatus.FINISHED_SUCCESS);
    log.info("Marked job '{}' as finished", getRegisterJobId());
  }

  private void markJobFailed(
      RegisterJobStatus jobStatus, List<ValidationError> validationErrors) {
    onBeforeMarkJobFailed();

    if (shouldMarkJobFailed()) {
      registerJobSupervisor.markFailureWithValidationErrors(
          getRegisterJobId(),
          jobStatus,
          validationErrors
      );
      log.warn("Marked job '{}' as failed with status '{}', the number of validation errors: {}",
          getRegisterJobId(), jobStatus, validationErrors.size());
    }
  }

  private void markJobRunning() {
    registerJobSupervisor.updateStatus(getRegisterJobId(), RegisterJobStatus.RUNNING);
  }

  private int getRegisterJobId() {
    return registerJobId;
  }

  private String getCorrelationId() {
    return correlationId;
  }
}
