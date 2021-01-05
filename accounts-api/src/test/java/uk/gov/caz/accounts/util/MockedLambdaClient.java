package uk.gov.caz.accounts.util;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class MockedLambdaClient implements LambdaClient {

  private InvokeRequest capturedInvokeRequest;

  @Override
  public InvokeResponse invoke(InvokeRequest invokeRequest)
      throws AwsServiceException, SdkClientException {
    capturedInvokeRequest = invokeRequest;
    return InvokeResponse.builder().build();
  }

  @Override
  public String serviceName() {
    return null;
  }

  @Override
  public void close() {
  }

  public InvokeRequest getCapturedInvokeRequest() {
    return capturedInvokeRequest;
  }
}
