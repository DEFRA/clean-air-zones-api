package uk.gov.caz.taxiregister.service;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;

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
  private final VehicleToLicenceConverter licenceConverter;
  private final LicencesRegistrationSecuritySentinel securitySentinel;

  /**
   * Creates an instance of {@link AbstractRegisterCommand}.
   */
  public AbstractRegisterCommand(RegisterServicesContext registerServicesContext, int registerJobId,
      String correlationId) {
    this.registerService = registerServicesContext.getRegisterService();
    this.exceptionResolver = registerServicesContext.getExceptionResolver();
    this.registerJobSupervisor = registerServicesContext.getRegisterJobSupervisor();
    this.licenceConverter = registerServicesContext.getLicenceConverter();
    this.maxValidationErrorCount = registerServicesContext.getMaxValidationErrorCount();
    this.securitySentinel = registerServicesContext.getSecuritySentinel();
    this.registerJobId = registerJobId;
    this.correlationId = correlationId;
  }

  /**
   * A hook-method which is executed once the registration job has been marked as 'running' and
   * before the actual registration takes place.
   */
  abstract void beforeExecute();

  /**
   * Gets {@code uploader-id}.
   *
   * @return {@link UUID} which represents the uploader.
   */
  abstract UUID getUploaderId();

  /**
   * Gets licences which are to be registered.
   *
   * @return A {@link List} of {@link VehicleDto} which are to be registered.
   */
  abstract List<VehicleDto> getLicencesToRegister();

  /**
   * Gets validation errors which occurred during parsing the input CSV file. For REST API call an
   * empty list should be returned.
   *
   * @return A {@link List} of {@link ValidationError} which represents the CSV parse validation
   *     errors.
   */
  abstract List<ValidationError> getLicencesParseValidationErrors();

  /**
   * Returns a boolean indicating whether the job should be marked as failed.
   */
  abstract boolean shouldMarkJobFailed();

  /**
   * Method hook before mark job failed.
   */
  abstract void onBeforeMarkJobFailed();

  /**
   * Method that executes logic common for all providers eg. S3 or REST API.
   *
   * @return {@link RegisterResult} register result of given rows.
   */
  public final RegisterResult execute() throws OutOfMemoryError {
    try {
      log.info("Processing registration, correlation-id: '{}' : start", getCorrelationId());

      markJobRunning();

      beforeExecute();

      // assertion: conversionMaxErrorCount >= 0
      int conversionMaxErrorCount = maxValidationErrorCount - parseValidationErrorCount();

      ConversionResults conversionResults = licenceConverter.convert(
          getLicencesToRegister(), conversionMaxErrorCount
      );

      RegisterResult potentialErroneousResult = checkForAnyErrors(conversionResults);
      if (potentialErroneousResult != null) {
        markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
            potentialErroneousResult.getValidationErrors());
        return potentialErroneousResult;
      }

      // Lock all Licensing Authorities
      // that the job is attempting to update
      lockImpactedLocalAuthority(conversionResults);

      RegisterResult result = registerService.register(
          conversionResults.getLicences(),
          getUploaderId()
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

  /**
   * Marks the registration job run as a failure.
   *
   * @param jobStatus A status which the job will be assigned to.
   * @param validationErrors A list of validation errors which caused the job to fail.
   */
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
    } else {
      log.error("Cannot delete file from S3 hence not updating status of the job in database.");
    }
  }

  /**
   * Checks if there were any errors during loading, parsing, converting or possible security
   * violations.
   *
   * @param conversionResults {@link uk.gov.caz.taxiregister.model.ConversionResult} that stores
   *     result of converting DTOs to models.
   * @return {@link RegisterResult} object with failure status in case of any error, null if there
   *     were no errors and registration can continue.
   */
  private RegisterResult checkForAnyErrors(ConversionResults conversionResults) {
    RegisterResult potentialErroneousResult = checkPermissions(conversionResults);
    if (potentialErroneousResult != null) {
      return potentialErroneousResult;
    }

    potentialErroneousResult = checkForParseAndValidationErrors(conversionResults);
    if (potentialErroneousResult != null) {
      return potentialErroneousResult;
    }

    potentialErroneousResult = checkLicensingAuthorityReadiness(conversionResults);
    if (potentialErroneousResult != null) {
      return potentialErroneousResult;
    }

    return null;
  }

  /**
   * Checks if there are any possible security violations in soon-to-begin register process.
   *
   * @param conversionResults {@link uk.gov.caz.taxiregister.model.ConversionResult} that stores
   *     result of converting DTOs to models.
   * @return {@link RegisterResult} object with failure status in case of any error, null if there
   *     were no errors and registration can continue.
   */
  private RegisterResult checkPermissions(ConversionResults conversionResults) {
    Set<LicensingAuthority> licensingAuthoritiesThisUploaderWantsToModify =
        collectDistinctLicensingAuthoritiesThatAreToBeUpdated(conversionResults);
    Optional<String> insufficientPermissionsError = securitySentinel
        .checkUploaderPermissionsToModifyLicensingAuthorities(
            getUploaderId(), licensingAuthoritiesThisUploaderWantsToModify);
    return insufficientPermissionsError
        .map(ValidationError::insufficientPermissionsError)
        .map(FailureRegisterResult::with)
        .orElse(null);
  }

  /**
   * Checks if Licensing Authorities are not currently locked by other jobs.
   *
   * @param conversionResults {@link uk.gov.caz.taxiregister.model.ConversionResult} that stores
   *     the list of Licensing Authorities.
   * @return {@link RegisterResult} object with failure status in case of any of give LA are locked
   *     null if there were no errors and registration can continue.
   */
  private RegisterResult checkLicensingAuthorityReadiness(ConversionResults conversionResults) {
    Set<LicensingAuthority> licensingAuthoritiesThisUploaderWantsToModify =
        collectDistinctLicensingAuthoritiesThatAreToBeUpdated(conversionResults);
    boolean hasActiveJobs = registerJobSupervisor
        .hasActiveJobs(licensingAuthoritiesThisUploaderWantsToModify);
    if (hasActiveJobs) {
      return FailureRegisterResult.with(ValidationError.licensingAuthorityUnavailabilityError());
    }
    return null;
  }

  /**
   * Lock impacted Local Authority to prevent other job from modifying their data.
   *
   * @param conversionResults {@link uk.gov.caz.taxiregister.model.ConversionResult} that stores
   *     the list of Licensing Authorities.
   */
  private void lockImpactedLocalAuthority(ConversionResults conversionResults)
      throws SQLException {
    Set<LicensingAuthority> affectedLicensingAuthorities =
        collectDistinctLicensingAuthoritiesThatAreToBeUpdated(conversionResults);
    registerJobSupervisor.lockImpactedLocalAuthorities(getRegisterJobId(),
        affectedLicensingAuthorities);
    log.info("Locked impacted authorities of job {} as finished", getRegisterJobId());
  }

  /**
   * Checks if there were any errors during loading, parsing or converting.
   *
   * @param conversionResults {@link uk.gov.caz.taxiregister.model.ConversionResult} that stores
   *     result of converting DTOs to models.
   * @return {@link RegisterResult} object with failure status in case of any error, null if there
   *     were no errors and registration can continue.
   */
  private RegisterResult checkForParseAndValidationErrors(ConversionResults conversionResults) {
    if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
      List<ValidationError> errors = merge(conversionResults.getValidationErrors(),
          getLicencesParseValidationErrors());
      return FailureRegisterResult.with(errors);
    }
    return null;
  }

  /**
   * Create and return a set of licensing authorities which are affected by the registration run.
   *
   * @param conversionResults A result of converting DTOs ({@link VehicleDto}) to model ({@link
   *     TaxiPhvVehicleLicence}).
   * @return A {@link Set} of {@link LicensingAuthority} which are affected by the given
   *     registration.
   */
  private Set<LicensingAuthority> collectDistinctLicensingAuthoritiesThatAreToBeUpdated(
      ConversionResults conversionResults) {
    return conversionResults.getLicences()
        .stream()
        .map(TaxiPhvVehicleLicence::getLicensingAuthority)
        .collect(Collectors.toSet());
  }

  /**
   * Check whether there are any CSV parse validation errors.
   *
   * @return true if there are CSV parse validation errors, false otherwise.
   */
  private boolean hasParseValidationErrors() {
    return !getLicencesParseValidationErrors().isEmpty();
  }

  /**
   * Gets the number of CSV parse validation errors.
   *
   * @return The number of CSV parse validation errors.
   */
  private int parseValidationErrorCount() {
    return getLicencesParseValidationErrors().size();
  }

  /**
   * Merges two lists passed as parameters.
   *
   * @param a The first list which is to be merged.
   * @param b The second list which is to be merged.
   * @return A concatenation of lists {@code a} and {@code b}.
   */
  private List<ValidationError> merge(List<ValidationError> a, List<ValidationError> b) {
    return Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toList());
  }

  /**
   * Once the registration has been completed marks the registration job as a success or failure.
   *
   * @param result The result of the registration.
   */
  private void postProcessRegistrationResult(RegisterResult result) {
    if (result.isSuccess()) {
      markJobFinished(result.getAffectedLicensingAuthorities());
    } else {
      markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
          result.getValidationErrors());
    }
  }

  /**
   * Marks the registration job run as a success.
   *
   * @param affectedLicensingAuthorities Licensing authorities affected by this registration
   *     run.
   */
  private void markJobFinished(Set<LicensingAuthority> affectedLicensingAuthorities) {
    registerJobSupervisor.markSuccessfullyFinished(getRegisterJobId(),
        affectedLicensingAuthorities);
    log.info("Marked job '{}' as finished", getRegisterJobId());
  }

  /**
   * Marks the given registration job as running.
   */
  private void markJobRunning() {
    registerJobSupervisor.updateStatus(getRegisterJobId(), RegisterJobStatus.RUNNING);
  }

  /**
   * Get the registration job ID.
   *
   * @return The ID of the registration job.
   */
  protected int getRegisterJobId() {
    return registerJobId;
  }

  /**
   * Gets job's correlation id.
   *
   * @return The correlation id of the given job.
   */
  private String getCorrelationId() {
    return correlationId;
  }
}
