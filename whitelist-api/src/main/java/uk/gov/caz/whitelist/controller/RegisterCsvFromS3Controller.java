package uk.gov.caz.whitelist.controller;

import static uk.gov.caz.whitelist.controller.Constants.CORRELATION_ID_HEADER;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.whitelist.dto.ErrorsResponse;
import uk.gov.caz.whitelist.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.whitelist.dto.RegisterJobStatusDto;
import uk.gov.caz.whitelist.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.whitelist.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.whitelist.model.CsvContentType;
import uk.gov.caz.whitelist.model.registerjob.RegisterJob;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobName;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.whitelist.service.AsyncBackgroundJobStarter;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.whitelist.service.RegisterJobSupervisor;
import uk.gov.caz.whitelist.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.whitelist.service.exception.ActiveJobsCountExceededException;
import uk.gov.caz.whitelist.service.exception.FatalErrorWithCsvFileMetadataException;

@RestController
@Slf4j
public class RegisterCsvFromS3Controller implements RegisterCsvFromS3ControllerApiSpec {

  public static final String PATH = "/v1/whitelisting/register-csv-from-s3/jobs";

  private final AsyncBackgroundJobStarter asyncBackgroundJobStarter;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final CsvFileOnS3MetadataExtractor metadataExtractor;

  /**
   * Creates new instance of {@link RegisterCsvFromS3Controller} class.
   *
   * @param asyncBackgroundJobStarter Implementation of {@link AsyncBackgroundJobStarter}
   *     interface.
   * @param registerJobSupervisor {@link RegisterJobSupervisor} that supervises whole job run.
   * @param csvFileOnS3MetadataExtractor {@link CsvFileOnS3MetadataExtractor} that allows to get
   *     'uploader-id' and 'csv-content-type' metadata from CSV file.
   */
  public RegisterCsvFromS3Controller(
      AsyncBackgroundJobStarter asyncBackgroundJobStarter,
      RegisterJobSupervisor registerJobSupervisor,
      CsvFileOnS3MetadataExtractor csvFileOnS3MetadataExtractor) {
    this.asyncBackgroundJobStarter = asyncBackgroundJobStarter;
    this.registerJobSupervisor = registerJobSupervisor;
    this.metadataExtractor = csvFileOnS3MetadataExtractor;
  }

  @Override
  public ResponseEntity<RegisterCsvFromS3JobHandle> startRegisterJob(
      String correlationId, StartRegisterCsvFromS3JobCommand startCommand) {
    CsvMetadata csvMetadata = getCsvMetadata(startCommand);

    checkPreconditions();

    StartParams startParams = prepareStartParams(correlationId, startCommand, csvMetadata);
    RegisterJobName registerJobName = registerJobSupervisor.start(startParams);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(new RegisterCsvFromS3JobHandle(registerJobName.getValue()));
  }

  private CsvMetadata getCsvMetadata(StartRegisterCsvFromS3JobCommand startCommand) {
    return metadataExtractor
        .getRequiredMetadata(startCommand.getS3Bucket(), startCommand.getFilename());
  }

  private void checkPreconditions() {
    checkActiveJobsPrecondition();
  }

  private void checkActiveJobsPrecondition() {
    if (registerJobSupervisor.hasActiveJobsFor(CsvContentType.WHITELIST_LIST)) {
      throw new ActiveJobsCountExceededException(CsvContentType.WHITELIST_LIST);
    }
  }

  private StartParams prepareStartParams(String correlationId,
      StartRegisterCsvFromS3JobCommand startRegisterCsvFromS3JobCommand, CsvMetadata csvMetadata) {
    return StartParams.builder()
        .registerJobTrigger(RegisterJobTrigger.from(CsvContentType.WHITELIST_LIST))
        .registerJobNameSuffix(
            FilenameUtils.removeExtension(startRegisterCsvFromS3JobCommand.getFilename()))
        .correlationId(correlationId)
        .uploaderId(csvMetadata.getUploaderId())
        .registerJobInvoker(
            asyncRegisterJobInvoker(correlationId, startRegisterCsvFromS3JobCommand))
        .build();
  }

  @Override
  public ResponseEntity<StatusOfRegisterCsvFromS3JobQueryResult> queryForStatusOfRegisterJob(
      String correlationId, String registerJobName) {

    Optional<RegisterJob> registerJobOptional = registerJobSupervisor
        .findJobWithName(new RegisterJobName(registerJobName));
    return registerJobOptional
        .map(registerJob -> ResponseEntity.ok()
            .header(CORRELATION_ID_HEADER, correlationId)
            .body(toQueryResult(registerJob)))
        .orElseGet(() -> ResponseEntity.notFound()
            .header(CORRELATION_ID_HEADER, correlationId)
            .build());
  }

  private RegisterJobSupervisor.RegisterJobInvoker asyncRegisterJobInvoker(String correlationId,
      StartRegisterCsvFromS3JobCommand startRegisterCsvFromS3JobCommand) {
    return registerJobId -> asyncBackgroundJobStarter.fireAndForgetRegisterCsvFromS3Job(
        registerJobId,
        startRegisterCsvFromS3JobCommand.getS3Bucket(),
        startRegisterCsvFromS3JobCommand.getFilename(),
        correlationId
    );
  }

  private StatusOfRegisterCsvFromS3JobQueryResult toQueryResult(RegisterJob registerJob) {
    RegisterJobStatusDto registerJobStatusDto = RegisterJobStatusDto.from(registerJob.getStatus());
    if (thereWereErrors(registerJob)) {
      return StatusOfRegisterCsvFromS3JobQueryResult
          .withStatusAndErrors(registerJobStatusDto, registerJob.getErrors());
    }
    return StatusOfRegisterCsvFromS3JobQueryResult.withStatusAndNoErrors(registerJobStatusDto);
  }

  private boolean thereWereErrors(RegisterJob registerJob) {
    return !registerJob.getErrors().isEmpty();
  }

  @ExceptionHandler(FatalErrorWithCsvFileMetadataException.class)
  ResponseEntity<String> handleFatalErrorWithCsvFileMetadataException(Exception e) {
    log.error(e.getMessage());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(e.getMessage());
  }

  @ExceptionHandler(ActiveJobsCountExceededException.class)
  ResponseEntity<ErrorsResponse> handleActiveJobCountExceededException(
      ActiveJobsCountExceededException e) {
    log.warn("Given content {} has already active job", e.getCsvContentType().name());
    return ResponseEntity
        .status(HttpStatus.NOT_ACCEPTABLE)
        .body(ErrorsResponse
            .singleValidationErrorResponse("Previous whitelist register job has not finished yet"));
  }
}
