package uk.gov.caz.accounts.amazonaws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.accounts.service.registerjob.AsyncBackgroundJobStarter;

/**
 * Asynchronously starts the register-job lambda in a fire-and-forget manner.
 */
@Component
@Profile("!development & !integration-tests")
@Slf4j
public class AsyncLambdaBackgroundJobStarter extends AbstractAsyncLambdaStarter implements
    AsyncBackgroundJobStarter {

  /**
   * Constructs new instance of {@link AsyncLambdaBackgroundJobStarter} class.
   *
   * @param objectMapper Jackson mapper.
   * @param lambdaClientBuilder An implementation of {@link LambdaClientBuilder} interface that
   *     will be used to get instance of {@link LambdaClient}.
   * @param lambdaName Name of Lambda function that should be invoked.
   */
  public AsyncLambdaBackgroundJobStarter(ObjectMapper objectMapper,
      LambdaClientBuilder lambdaClientBuilder,
      @Value("${registerjob.lambda.name}") String lambdaName) {
    super(objectMapper, lambdaClientBuilder, lambdaName);
  }

  @Override
  public void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId, boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    logCallDetails(registerJobId, s3Bucket, fileName, correlationId);
    try {
      String lambdaJsonPayload = prepareLambdaJsonPayload(registerJobId, s3Bucket, fileName,
          correlationId, shouldSendEmailsUponSuccessfulChargeCalculation);
      InvokeRequest invokeRequest = prepareInvokeRequestForFunction(lambdaName, lambdaJsonPayload);
      invokeLambda(invokeRequest);
    } catch (Exception e) {
      log.error("Error during invoking '" + lambdaName + "' Lambda", e);
    }
  }

  private void logCallDetails(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    log.info("Starting Async, fire and forget, Register job with parameters: JobID: {}, "
            + "S3 Bucket: {}, CSV File: {}, Correlation: {} and runner implementation: {}",
        registerJobId, s3Bucket, fileName, correlationId,
        AsyncLambdaBackgroundJobStarter.class.getSimpleName());
  }

  @SneakyThrows
  private String prepareLambdaJsonPayload(int registerJobId, String s3Bucket, String fileName,
      String correlationId, boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    RegisterCsvFromS3LambdaInput input = RegisterCsvFromS3LambdaInput.builder()
        .registerJobId(registerJobId)
        .s3Bucket(s3Bucket)
        .fileName(fileName)
        .correlationId(correlationId)
        .shouldSendEmailsUponSuccessfulChargeCalculation(
            shouldSendEmailsUponSuccessfulJobCompletion)
        .build();
    return objectMapper.writeValueAsString(input);
  }
}
