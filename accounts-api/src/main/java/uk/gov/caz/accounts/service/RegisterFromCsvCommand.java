package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.dto.CsvFindResult;
import uk.gov.caz.accounts.model.ConversionResults;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.ValidationError;
import uk.gov.caz.accounts.repository.AccountVehicleDtoCsvRepository;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationStarter;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;
import uk.gov.caz.accounts.service.registerjob.RegisterResult;
import uk.gov.caz.accounts.service.registerjob.RegisterService;
import uk.gov.caz.csv.model.CsvValidationError;

@Slf4j
public class RegisterFromCsvCommand {

  private final int registerJobId;
  private final int maxValidationErrorCount;
  private final String correlationId;
  private final String bucket;
  private final String filename;
  private final boolean shouldSendEmailsUponSuccessfulChargeCalculation;

  private final RegisterService registerService;
  private final RegisterFromCsvExceptionResolver exceptionResolver;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final AccountVehicleDtoCsvRepository accountVehicleDtoCsvRepository;
  private final AccountVehicleDtoToModelConverter dtoToModelConverter;
  private final AsyncChargeCalculationStarter asyncChargeCalculationStarter;

  private CsvFindResult csvFindResult;

  private boolean hasFileBeenPurgedFromS3;

  /**
   * Creates an instance of {@link RegisterFromCsvCommand}.
   */
  public RegisterFromCsvCommand(RegisterServiceContext registerServiceContext, int registerJobId,
      String correlationId, String bucket, String filename,
      boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    this.registerService = registerServiceContext.getRegisterService();
    this.exceptionResolver = registerServiceContext.getExceptionResolver();
    this.registerJobSupervisor = registerServiceContext.getRegisterJobSupervisor();
    this.accountVehicleDtoCsvRepository = registerServiceContext.getCsvRepository();
    this.maxValidationErrorCount = registerServiceContext.getMaxValidationErrorCount();
    this.dtoToModelConverter = registerServiceContext.getDtoToModelConverter();
    this.asyncChargeCalculationStarter = registerServiceContext
        .getAsyncChargeCalculationStarter();
    this.bucket = bucket;
    this.filename = filename;
    this.registerJobId = registerJobId;
    this.correlationId = correlationId;
    this.shouldSendEmailsUponSuccessfulChargeCalculation =
        shouldSendEmailsUponSuccessfulChargeCalculation;
  }

  /**
   * Registers vehicles whose data is located at S3 in bucket {@code bucket} and key {@code
   * filename}.
   */
  public RegisterResult register() {
    try {
      log.info("Processing registration, correlation-id: {} : start", correlationId);

      markJobRunning();

      beforeExecute();

      checkNotEmptyVehiclesPrecondition();

      ConversionResults conversionResults = dtoToModelConverter.convert(getVehiclesToRegister(),
          uploaderId());

      if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
        List<ValidationError> errors = combineValidationErrors(getParseValidationErrors(),
            conversionResults.getValidationErrors());
        markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, errors);
        return RegisterResult.failure(errors);
      }

      RegisterResult result = registerService.register(conversionResults.getAccountVehicles(),
          csvFindResult.getUploaderId());

      postProcessRegistrationResult(result);

      return result;
    } catch (Exception e) {
      RegisterResult result = exceptionResolver.resolve(e);
      markJobFailed(exceptionResolver.resolveToRegisterJobFailureStatus(e),
          result.getValidationErrors());
      return result;
    } finally {
      log.info("Processing registration, correlation-id: '{}' : finish", correlationId);
    }
  }

  private List<ValidationError> combineValidationErrors(List<ValidationError> parseValidationErrors,
      List<ValidationError> validationErrors) {
    return merge(validationErrors,
        parseValidationErrors)
        .stream()
        .sorted(Comparator.comparing(validationError -> validationError.getLineNumber().orElse(0)))
        .limit(maxValidationErrorCount)
        .collect(Collectors.toList());
  }

  public void beforeExecute() {
    csvFindResult = accountVehicleDtoCsvRepository.findAll(bucket, filename);
  }

  private void checkNotEmptyVehiclesPrecondition() {
    checkCsvParseResultsPresentPrecondition();
    if (csvFindResult.getVehicles().isEmpty()) {
      throw new InvalidRequestPayloadException("No account vehicles given.");
    }
  }

  List<ValidationError> getParseValidationErrors() {
    checkCsvParseResultsPresentPrecondition();
    return toValidationErrors(csvFindResult.getValidationErrors());
  }

  List<AccountVehicleDto> getVehiclesToRegister() {
    checkCsvParseResultsPresentPrecondition();
    return csvFindResult.getVehicles();
  }

  private void checkCsvParseResultsPresentPrecondition() {
    Preconditions.checkState(csvFindResult != null, "CSV parse results need to obtained first");
  }

  private boolean hasParseValidationErrors() {
    return !getParseValidationErrors().isEmpty();
  }

  private List<ValidationError> merge(List<ValidationError> a, List<ValidationError> b) {
    return Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toList());
  }

  private void postProcessRegistrationResult(RegisterResult result) {
    if (result.isSuccess()) {
      markJobChargeabilityCalculationInProgress();
      asyncChargeCalculationStarter.fireAndForget(uploaderId(),
          registerJobId,
          UUID.fromString(correlationId),
          1,
          shouldSendEmailsUponSuccessfulChargeCalculation
      );
    } else {
      markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
          result.getValidationErrors());
    }
  }

  private UUID uploaderId() {
    return csvFindResult.getUploaderId();
  }

  private void markJobRunning() {
    registerJobSupervisor.updateStatus(registerJobId, RegisterJobStatus.RUNNING);
  }

  private void markJobChargeabilityCalculationInProgress() {
    registerJobSupervisor
        .updateStatus(registerJobId, RegisterJobStatus.CHARGEABILITY_CALCULATION_IN_PROGRESS);
    log.info("Marked job '{}' as chargeability calculation in progress", registerJobId);
  }

  private void markJobFailed(RegisterJobStatus jobStatus, List<ValidationError> validationErrors) {
    onBeforeMarkJobFailed();

    if (shouldMarkJobFailed()) {
      registerJobSupervisor.markFailureWithValidationErrors(
          registerJobId,
          jobStatus,
          validationErrors
      );
      log.warn("Marked job '{}' as failed with status '{}', the number of validation errors: {}",
          registerJobId, jobStatus, validationErrors.size());
    } else {
      log.error("Cannot delete file from S3 hence not updating status of the job in database.");
    }
  }

  private void onBeforeMarkJobFailed() {
    hasFileBeenPurgedFromS3 = accountVehicleDtoCsvRepository.purgeFile(bucket, filename);
  }

  private boolean shouldMarkJobFailed() {
    return hasFileBeenPurgedFromS3;
  }

  private List<ValidationError> toValidationErrors(List<CsvValidationError> validationErrors) {
    return validationErrors.stream()
        .map(ValidationError::valueErrorFrom)
        .collect(Collectors.toList());
  }
}
