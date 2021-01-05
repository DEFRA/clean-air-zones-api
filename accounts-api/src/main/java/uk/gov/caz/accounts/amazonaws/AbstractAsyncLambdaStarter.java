package uk.gov.caz.accounts.amazonaws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * Base class for async lambda starters.
 */
@Slf4j
public abstract class AbstractAsyncLambdaStarter {

  protected final ObjectMapper objectMapper;
  private final LambdaClientBuilder lambdaClientBuilder;
  protected final String lambdaName;

  /**
   * Constructs new instance of {@link AbstractAsyncLambdaStarter} class.
   *
   * @param objectMapper Jackson mapper.
   * @param lambdaClientBuilder An implementation of {@link LambdaClientBuilder} interface that
   *     will be used to get instance of {@link LambdaClient}.
   */
  public AbstractAsyncLambdaStarter(ObjectMapper objectMapper,
      LambdaClientBuilder lambdaClientBuilder, String lambdaName) {
    this.objectMapper = objectMapper;
    this.lambdaClientBuilder = lambdaClientBuilder;
    this.lambdaName = lambdaName;
  }

  /**
   * Prepares a request object for AWS SDK lambda invocation.
   */
  protected InvokeRequest prepareInvokeRequestForFunction(String lambdaFunctionName,
      String lambdaJsonPayload) {
    SdkBytes payloadSdkBytes = SdkBytes.fromUtf8String(lambdaJsonPayload);
    return InvokeRequest.builder()
        .invocationType(InvocationType.EVENT)
        .functionName(lambdaFunctionName)
        .payload(payloadSdkBytes)
        .build();
  }

  /**
   * Asynchronously invokes AWS lambda.
   */
  protected void invokeLambda(InvokeRequest invokeRequest) {
    try (LambdaClient lambdaClient = lambdaClientBuilder.build()) {
      InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
      log.info("Successfully invoked (asynchronously) '{}' Lambda. InvokeResponse: {}",
          lambdaName, invokeResponse);
    }
  }
}
