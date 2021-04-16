package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.dto.JobFailureData;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;

/**
 * Class which is responsible for registering vehicles whose data is located at S3.
 */
@Slf4j
public class RegisterFromCsvCommand extends AbstractRegisterCommand {

  private final EmailSendingValidationErrorsHandler emailSendingValidationErrorsHandler;
  private final String bucket;
  private final String filename;

  private final TaxiPhvLicenceCsvRepository csvRepository;

  private CsvFindResult csvFindResult;
  private List<ValidationError> parseValidationErrors;
  private boolean shouldPurgeFileFromS3;

  /**
   * Creates an instance of {@link RegisterFromCsvCommand}.
   */
  public RegisterFromCsvCommand(
      RegisterServicesContext registerServicesContext,
      int registerJobId,
      String correlationId, String bucket, String filename) {
    super(registerServicesContext, registerJobId, correlationId);
    this.emailSendingValidationErrorsHandler = registerServicesContext
        .getEmailSendingValidationErrorsHandler();
    this.bucket = bucket;
    this.filename = filename;
    this.csvRepository = registerServicesContext.getCsvRepository();
  }

  @Override
  public void beforeExecute() {
    this.csvFindResult = csvRepository.findAll(bucket, filename);
    this.parseValidationErrors = toValidationErrors(csvFindResult.getValidationErrors());
  }

  private List<ValidationError> toValidationErrors(List<CsvValidationError> validationErrors) {
    return validationErrors.stream()
        .map(ValidationError::valueErrorFrom)
        .collect(Collectors.toList());
  }

  @Override
  public UUID getUploaderId() {
    checkCsvParseResultsPresentPrecondition();
    return csvFindResult.getUploaderId();
  }

  @Override
  public List<VehicleDto> getLicencesToRegister() {
    checkCsvParseResultsPresentPrecondition();
    return csvFindResult.getLicences();
  }

  @Override
  List<ValidationError> getLicencesParseValidationErrors() {
    checkCsvParseResultsPresentPrecondition();
    return parseValidationErrors;
  }

  @Override
  boolean shouldMarkJobFailed() {
    return shouldPurgeFileFromS3;
  }

  @Override
  void onBeforeMarkJobFailed(RegisterJobStatus jobStatus,
      List<ValidationError> validationErrors) {
    List<ValidationError> sortedErrors = validationErrors.stream()
        .sorted()
        .collect(Collectors.toList());
    shouldPurgeFileFromS3 = csvRepository.purgeFile(bucket, filename);
    emailSendingValidationErrorsHandler.handle(JobFailureData.builder()
        .ownerEmail(getUploaderEmail())
        .jobStatus(jobStatus)
        .validationErrors(sortedErrors)
        .build()
    );
  }

  @Override
  void afterMarkJobFinished(ConversionResults conversionResults) {
    // nothing to be done here when registering from CSV
  }

  private void checkCsvParseResultsPresentPrecondition() {
    Preconditions.checkState(csvFindResult != null, "CSV parse results need to obtained first");
  }

  private Optional<String> getUploaderEmail() {
    return Optional.ofNullable(csvFindResult).map(CsvFindResult::getUploaderEmail);
  }
}