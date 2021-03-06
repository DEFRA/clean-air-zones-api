package uk.gov.caz.taxiregister.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.caz.util.function.MdcAwareSupplier;

/**
 * Starts the background task which registers Taxi and PHV licences from CSV file located at S3
 * in the {@link ForkJoinPool#commonPool()}.
 */
@AllArgsConstructor
@Component
@Profile("development | integration-tests")
@Slf4j
public class AsyncJavaBackgroundJobStarter implements AsyncBackgroundJobStarter {

  private final SourceAwareRegisterService registerService;

  @Override
  public void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    logCallDetails(registerJobId, s3Bucket, fileName, correlationId);
    CompletableFuture.supplyAsync(
        MdcAwareSupplier.from(
            () -> registerService.register(s3Bucket, fileName, registerJobId, correlationId)
        )
    );
  }

  /**
   * Logs details of the call which is to be made.
   */
  private void logCallDetails(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    log.info(
        "Starting Async, fire and forget, Register job with parameters: JobID: {}, S3 Bucket: {}, "
            + "CSV File: {}, Correlation: {} and runner implementation: {}",
        registerJobId, s3Bucket, fileName, correlationId,
        AsyncJavaBackgroundJobStarter.class.getSimpleName());
  }

}
