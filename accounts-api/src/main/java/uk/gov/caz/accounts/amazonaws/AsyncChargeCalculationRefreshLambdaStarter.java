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
import uk.gov.caz.accounts.dto.ChargeCalculationRefreshLambdaInput;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationRefreshStarter;

/**
 * Asynchronously starts the charge calculation refresh lambda in a fire-and-forget manner.
 */
@Component
@Profile("!development & !integration-tests")
@Slf4j
public class AsyncChargeCalculationRefreshLambdaStarter extends
    AbstractAsyncLambdaStarter implements
    AsyncChargeCalculationRefreshStarter {

  /**
   * Constructs new instance of {@link AsyncChargeCalculationRefreshLambdaStarter} class.
   *
   * @param objectMapper Jackson mapper.
   * @param lambdaClientBuilder An implementation of {@link LambdaClientBuilder} interface that
   *     will be used to get instance of {@link LambdaClient}.
   * @param lambdaName Name of Lambda function that should be invoked.
   */
  public AsyncChargeCalculationRefreshLambdaStarter(ObjectMapper objectMapper,
      LambdaClientBuilder lambdaClientBuilder,
      @Value("${charge-calculation.lambda.name}") String lambdaName) {
    super(objectMapper, lambdaClientBuilder, lambdaName);
  }

  /**
   * Asynchronously invoke Charge Calculation refresh lambda.
   *
   * @param correlationId Correlation ID to track requests.
   * @param newInvocationNumber Integer specifying how many times the given lambda has already
   *     been invoked (including the given call).
   */
  @Override
  public void fireAndForget(UUID correlationId, int newInvocationNumber) {
    logCallDetails(correlationId);
    try {
      String lambdaJsonPayload = prepareLambdaJsonPayload(correlationId, newInvocationNumber);
      InvokeRequest invokeRequest = prepareInvokeRequestForFunction(lambdaName, lambdaJsonPayload);
      invokeLambda(invokeRequest);
    } catch (Exception e) {
      log.error("Error during invoking '" + lambdaName + "' Lambda", e);
    }
  }

  /**
   * Logs invocation details.
   */
  private void logCallDetails(UUID correlationId) {
    log.info(
        "Starting Async, fire and forget, Charge Calculation Refresh Lambda with Correlation: {}",
        correlationId);
  }

  /**
   * Prepare JSON String with Lambda input.
   */
  @SneakyThrows
  private String prepareLambdaJsonPayload(UUID correlationId, int newInvocationNumber) {
    ChargeCalculationRefreshLambdaInput input = ChargeCalculationRefreshLambdaInput.builder()
        .correlationId(correlationId)
        .invocationNumber(newInvocationNumber)
        .build();
    return objectMapper.writeValueAsString(input);
  }
}
