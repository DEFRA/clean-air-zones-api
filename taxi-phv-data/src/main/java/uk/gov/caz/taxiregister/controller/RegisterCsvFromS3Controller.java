package uk.gov.caz.taxiregister.controller;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.controller.exception.UnableToGetUploaderIdMetadataException;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.taxiregister.dto.RegisterJobStatusDto;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.S3FileMetadataExtractor;

@RestController
@Slf4j
public class RegisterCsvFromS3Controller implements RegisterCsvFromS3ControllerApiSpec {

  public static final String PATH = "/v1/scheme-management/register-csv-from-s3/jobs";

  private static final String UPLOAD_UNSUCCESSFUL_YOU_WILL_RECEIVE_AN_EMAIL_WITH_ERRORS
      = "CSV file upload unsuccessful. You will receive an email with error messages detailed.";

  private final AsyncBackgroundJobStarter asyncBackgroundJobStarter;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final S3FileMetadataExtractor s3FileMetadataExtractor;

  @Value("${application.mail.allowed-errors-before-sending-email}")
  private int allowedErrorsBeforeSendingEmail;

  /**
   * Creates new instance of {@link RegisterCsvFromS3Controller} class.
   *
   * @param asyncBackgroundJobStarter Implementation of {@link AsyncBackgroundJobStarter}
   *     interface.
   * @param registerJobSupervisor {@link RegisterJobSupervisor} that supervises whole job run.
   * @param s3FileMetadataExtractor {@link S3FileMetadataExtractor} that allows to get
   *     'uploader-id' metadata from CSV file.
   */
  public RegisterCsvFromS3Controller(
      AsyncBackgroundJobStarter asyncBackgroundJobStarter,
      RegisterJobSupervisor registerJobSupervisor,
      S3FileMetadataExtractor s3FileMetadataExtractor) {
    this.asyncBackgroundJobStarter = asyncBackgroundJobStarter;
    this.registerJobSupervisor = registerJobSupervisor;
    this.s3FileMetadataExtractor = s3FileMetadataExtractor;
  }

  @Override
  public ResponseEntity<RegisterCsvFromS3JobHandle> startRegisterJob(
      String correlationId, StartRegisterCsvFromS3JobCommand startCommand) {
    UUID uploaderId = getUploaderIdOrThrowIfUnableTo(startCommand);

    StartParams startParams = prepareStartParams(correlationId, startCommand, uploaderId);
    RegisterJobName registerJobName = registerJobSupervisor.start(startParams);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(new RegisterCsvFromS3JobHandle(registerJobName.getValue()));
  }

  private UUID getUploaderIdOrThrowIfUnableTo(
      StartRegisterCsvFromS3JobCommand startCommand) {
    Optional<UUID> uploaderId = s3FileMetadataExtractor
        .getUploaderId(startCommand.getS3Bucket(), startCommand.getFilename());

    return uploaderId.orElseThrow(
        () -> new UnableToGetUploaderIdMetadataException(prepareErrorMessage(startCommand)));
  }

  private String prepareErrorMessage(StartRegisterCsvFromS3JobCommand startCommand) {
    return "Unable to fetch \"uploader-id\" metadata from S3 Bucket: " + startCommand.getS3Bucket()
        + "; File: " + startCommand.getFilename();
  }

  @Override
  public ResponseEntity<StatusOfRegisterCsvFromS3JobQueryResult> queryForStatusOfRegisterJob(
      String registerJobName) {

    Optional<RegisterJob> registerJobOptional = registerJobSupervisor
        .findJobWithName(new RegisterJobName(registerJobName));
    return registerJobOptional
        .map(registerJob -> ResponseEntity.ok()
            .body(toQueryResult(registerJob)))
        .orElseGet(() -> ResponseEntity.notFound()
            .build());
  }

  private StartParams prepareStartParams(String correlationId,
      StartRegisterCsvFromS3JobCommand startRegisterCsvFromS3JobCommand, UUID uploaderId) {
    return StartParams.builder()
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .registerJobNameSuffix(
            FilenameUtils.removeExtension(startRegisterCsvFromS3JobCommand.getFilename()))
        .correlationId(correlationId)
        .uploaderId(uploaderId)
        .registerJobInvoker(
            asyncRegisterJobInvoker(correlationId, startRegisterCsvFromS3JobCommand))
        .build();
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
      long registerJobErrorsCount = registerJob.getErrors().stream()
          .filter(error -> isNotBlank(error.getDetail()))
          .count();
      if (registerJobErrorsCount > allowedErrorsBeforeSendingEmail) {
        return StatusOfRegisterCsvFromS3JobQueryResult
            .withStatusAndSpecificError(registerJobStatusDto,
                UPLOAD_UNSUCCESSFUL_YOU_WILL_RECEIVE_AN_EMAIL_WITH_ERRORS);
      }
      return StatusOfRegisterCsvFromS3JobQueryResult
          .withStatusAndErrors(registerJobStatusDto, registerJob.getErrors());
    }
    return StatusOfRegisterCsvFromS3JobQueryResult.withStatusAndNoErrors(registerJobStatusDto);
  }

  private boolean thereWereErrors(RegisterJob registerJob) {
    return !registerJob.getErrors().isEmpty();
  }
}
