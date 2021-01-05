package uk.gov.caz.accounts.controller;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.accounts.dto.RegisterJobStatusDto;
import uk.gov.caz.accounts.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.accounts.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.accounts.model.registerjob.RegisterJob;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.service.registerjob.RegisterJobStarter;
import uk.gov.caz.accounts.service.registerjob.RegisterJobStarter.InitialJobParams;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;

/**
 * Rest controller that allows fleets to start jobs that registers VRNs into the database from a CSV
 * file located at AWS S3.
 */
@RestController
@AllArgsConstructor
public class RegisterCsvFromS3Controller implements RegisterCsvFromS3ControllerApiSpec {

  public static final String PATH = "/v1/accounts/register-csv-from-s3/jobs";

  private final RegisterJobStarter registerJobStarter;
  private final RegisterJobSupervisor registerJobSupervisor;

  @Override
  public ResponseEntity<RegisterCsvFromS3JobHandle> startRegisterJob(String correlationId,
      StartRegisterCsvFromS3JobCommand startCommand) {
    startCommand.validate();

    InitialJobParams jobParams = toInitialJobParams(correlationId, startCommand);
    RegisterJobName jobName = registerJobStarter.start(jobParams);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterCsvFromS3JobHandle.of(jobName.getValue()));
  }

  /**
   * Creates an instance of {@link InitialJobParams} based on provided parameters.
   */
  private InitialJobParams toInitialJobParams(String correlationId,
      StartRegisterCsvFromS3JobCommand startCommand) {
    return InitialJobParams.builder()
        .s3Bucket(startCommand.getS3Bucket())
        .filename(startCommand.getFilename())
        .correlationId(correlationId)
        .sendEmails(startCommand.shouldSendEmailsUponSuccessfulJobCompletion())
        .build();
  }

  @Override
  public ResponseEntity<StatusOfRegisterCsvFromS3JobQueryResult> queryForStatusOfRegisterJob(
      String correlationId, String registerJobName) {

    Optional<RegisterJob> registerJobOptional = registerJobSupervisor
        .findJobWithName(new RegisterJobName(registerJobName));

    return registerJobOptional
        .map(registerJob -> ResponseEntity.ok().body(toQueryResult(registerJob)))
        .orElseGet(() -> ResponseEntity.notFound().build());
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
    return registerJob.getErrors().hasErrors();
  }
}