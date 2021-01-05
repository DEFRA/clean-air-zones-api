package uk.gov.caz.accounts.amazonaws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import uk.gov.caz.accounts.dto.ChargeCalculationLambdaInput;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationStarter;

/**
 * Asynchronously starts the charge calculation lambda in a fire-and-forget manner.
 */
@Component
@Profile("!development & !integration-tests")
@Slf4j
public class AsyncChargeCalculationLambdaStarter extends AbstractAsyncLambdaStarter implements
    AsyncChargeCalculationStarter {

  /**
   * Constructs new instance of {@link AsyncChargeCalculationLambdaStarter} class.
   *
   * @param objectMapper Jackson mapper.
   * @param lambdaClientBuilder An implementation of {@link LambdaClientBuilder} interface that
   *     will be used to get instance of {@link LambdaClient}.
   * @param lambdaName Name of Lambda function that should be invoked.
   */
  public AsyncChargeCalculationLambdaStarter(ObjectMapper objectMapper,
      LambdaClientBuilder lambdaClientBuilder,
      @Value("${charge-calculation.lambda.name}") String lambdaName) {
    super(objectMapper, lambdaClientBuilder, lambdaName);
  }

  /**
   * Asynchronously invoke Charge Calculation lambda.
   *
   * @param accountId Account/Fleet ID.
   * @param jobId ID of register CSV job that is in progress. Can be null if lambda invoked due
   *     to API call to add single vehicle instead of CSV import job.
   * @param correlationId Correlation ID to track requests.
   * @param newInvocationNumber Integer specifying how many times the given lambda has already
   *     been invoked (including the given call).
   * @param shouldSendEmailsUponSuccessfulChargeCalculation Flag indicating whether to send
   *     email(s) upon successful charge-calculation-job completion.
   */
  @Override
  public void fireAndForget(UUID accountId, Integer jobId, UUID correlationId,
      int newInvocationNumber, boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    logCallDetails(accountId, jobId, correlationId);
    try {
      String lambdaJsonPayload = prepareLambdaJsonPayload(accountId, jobId, correlationId,
          newInvocationNumber, shouldSendEmailsUponSuccessfulChargeCalculation);
      InvokeRequest invokeRequest = prepareInvokeRequestForFunction(lambdaName, lambdaJsonPayload);
      invokeLambda(invokeRequest);
    } catch (Exception e) {
      log.error("Error during invoking '" + lambdaName + "' Lambda", e);
    }
  }

  /**
   * Logs invocation details.
   */
  private void logCallDetails(UUID accountId, Integer jobId, UUID correlationId) {
    log.info(
        "Starting Async, fire and forget, Charge Calculation Lambda with parameters: AccountID: {}"
            + ", JobId: {}, Correlation: {}", accountId, jobId, correlationId);
  }

  /**
   * Prepare JSON String with Lambda input.
   */
  @SneakyThrows
  private String prepareLambdaJsonPayload(UUID accountID, Integer jobId,
      UUID correlationId, int newInvocationNumber,
      boolean shouldSendEmailsUponSuccessfulChargeCalculation) {
    ChargeCalculationLambdaInput input = ChargeCalculationLambdaInput.builder()
        .accountId(accountID)
        .jobId(jobId)
        .correlationId(correlationId)
        .invocationNumber(newInvocationNumber)
        .shouldSendEmailsUponSuccessfulJobCompletion(
            shouldSendEmailsUponSuccessfulChargeCalculation)
        .build();
    return objectMapper.writeValueAsString(input);
  }
}
