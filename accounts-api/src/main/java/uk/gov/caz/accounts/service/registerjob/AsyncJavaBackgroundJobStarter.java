package uk.gov.caz.accounts.service.registerjob;

import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.service.RegisterFromCsvCommand;
import uk.gov.caz.accounts.service.RegisterServiceContext;
import uk.gov.caz.util.function.MdcAwareSupplier;

/**
 * Starts the register-from-csv-file job as a task in a separate thread in a fire-and-forget manner.
 */
@AllArgsConstructor
@Component
@Profile("development | integration-tests")
@Slf4j
public class AsyncJavaBackgroundJobStarter implements AsyncBackgroundJobStarter {

  private final RegisterServiceContext registerServiceContext;

  @Override
  public void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId, boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    logCallDetails(registerJobId, s3Bucket, fileName, correlationId);
    CompletableFuture.supplyAsync(MdcAwareSupplier.from(
        () -> new RegisterFromCsvCommand(
            registerServiceContext,
            registerJobId,
            correlationId,
            s3Bucket,
            fileName,
            shouldSendEmailsUponSuccessfulChargeCalculation
        ).register())
    );
  }

  private void logCallDetails(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    log.info("Starting Async, fire and forget, Register job with parameters: JobID: {}, "
            + "S3 Bucket: {}, CSV File: {}, Correlation: {} and runner implementation: {}",
        registerJobId, s3Bucket, fileName, correlationId,
        AsyncJavaBackgroundJobStarter.class.getSimpleName());
  }
}
