package uk.gov.caz.accounts.service.registerjob;

import com.google.common.collect.ImmutableList;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.RegisterJobRepository;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor.StartParams;
import uk.gov.caz.accounts.service.registerjob.exception.ActiveJobsCountExceededException;

/**
 * Validates and starts the register-csv-from-s3 jobs.
 */
@Service
@AllArgsConstructor
@Slf4j
public class RegisterJobStarter {

  private final CsvFileOnS3MetadataExtractor csvFileOnS3MetadataExtractor;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final AsyncBackgroundJobStarter asyncBackgroundJobStarter;
  private final RegisterJobRepository registerJobRepository;
  private final AccountUserRepository accountUserRepository;

  @Value
  @Builder
  public static class InitialJobParams {

    String correlationId;
    String s3Bucket;
    String filename;
    boolean sendEmails;

    public boolean shouldSendEmails() {
      return sendEmails;
    }
  }

  /**
   * Validates and starts the register-csv-from-s3 job mapped from by {@code jobParams}.
   *
   * @param jobParams Parameters of the job which is supposed by to be ran.
   * @return The unique job name.
   */
  public RegisterJobName start(InitialJobParams jobParams) {
    UUID accountId = extractAccountId(jobParams);
    verifyNoJobIsRunningSimultaneouslyForTheSame(accountId);

    StartParams startParams = prepareStartParams(jobParams, accountId);
    return registerJobSupervisor.start(startParams);
  }

  /**
   * Verifies there is none unfinished jobs for account with id {@code accountId}. {@link
   * ActiveJobsCountExceededException} is thrown if there are.
   */
  private void verifyNoJobIsRunningSimultaneouslyForTheSame(UUID accountId) {
    long simultaneousJobsCount = registerJobRepository.countAllByUploaderIdAndStatusIn(accountId,
        ImmutableList.of(RegisterJobStatus.STARTING, RegisterJobStatus.RUNNING));
    if (simultaneousJobsCount > 0) {
      log.warn("There is/are {} simultaneous job(s) for account '{}'", simultaneousJobsCount,
          accountId);
      throw new ActiveJobsCountExceededException();
    }
  }

  /**
   * Extracts {@code account-user-id} metadata from the file at S3 and maps it to associated {@code
   * account-id}.
   */
  private UUID extractAccountId(InitialJobParams jobParams) {
    UUID accountUserId = csvFileOnS3MetadataExtractor.getAccountUserId(jobParams.getS3Bucket(),
        jobParams.getFilename());
    return extractAccountIdByAccountUserId(accountUserId);
  }

  /**
   * Maps {@code accountUserId} of a User to the identifier of the associated {@link
   * uk.gov.caz.accounts.model.Account}. If the user is not found, {@link IllegalStateException} is
   * thrown.
   */
  private UUID extractAccountIdByAccountUserId(UUID accountUserId) {
    return accountUserRepository.findById(accountUserId)
        .map(User::getAccountId)
        .orElseThrow(() -> new IllegalStateException("User (id: " + accountUserId + ") not found"));
  }

  /**
   * Maps the passed params to an instance of {@link StartParams}.
   */
  private StartParams prepareStartParams(InitialJobParams jobParams, UUID uploaderId) {
    return StartParams.builder()
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .registerJobNameSuffix(FilenameUtils.removeExtension(jobParams.getFilename()))
        .correlationId(jobParams.getCorrelationId())
        .uploaderId(uploaderId)
        .registerJobInvoker(asyncRegisterJobInvoker(jobParams))
        .build();
  }

  /**
   * Maps {@code jobParams} to {@link RegisterJobSupervisor.RegisterJobInvoker}.
   */
  private RegisterJobSupervisor.RegisterJobInvoker asyncRegisterJobInvoker(
      InitialJobParams jobParams) {
    return registerJobId -> asyncBackgroundJobStarter.fireAndForgetRegisterCsvFromS3Job(
        registerJobId,
        jobParams.getS3Bucket(),
        jobParams.getFilename(),
        jobParams.getCorrelationId(),
        jobParams.shouldSendEmails()
    );
  }
}
