package uk.gov.caz.accounts.amazonaws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import uk.gov.caz.accounts.dto.ChargeCalculationRefreshLambdaInput;
import uk.gov.caz.accounts.util.MockedLambdaClient;

class AsyncChargeCalculationRefreshLambdaStarterTest {

  private static final String LAMBDA_NAME = "ChargeCalculationRefreshLambda";
  private static final UUID CORRELATION_ID = UUID.randomUUID();

  private ObjectMapper mockedObjectMapper;
  private LambdaClientBuilder mockedLambdaClientBuilder;
  private AsyncChargeCalculationRefreshLambdaStarter lambdaStarter;

  @BeforeEach
  public void init() {
    mockedObjectMapper = mock(ObjectMapper.class);
    mockedLambdaClientBuilder = mock(LambdaClientBuilder.class);
    lambdaStarter = new AsyncChargeCalculationRefreshLambdaStarter(mockedObjectMapper,
        mockedLambdaClientBuilder, LAMBDA_NAME);
  }

  @Test
  public void startingLambdaShouldPrepareContextAndDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    ChargeCalculationRefreshLambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input)).willReturn("payload");

    MockedLambdaClient mockedLambdaClient = new MockedLambdaClient();
    given(mockedLambdaClientBuilder.build()).willReturn(mockedLambdaClient);

    // when
    lambdaStarter.fireAndForget(CORRELATION_ID, 1);

    // then
    InvokeRequest capturedInvokeRequest = mockedLambdaClient.getCapturedInvokeRequest();
    assertThat(capturedInvokeRequest).isNotNull();
    assertThat(capturedInvokeRequest.functionName()).isEqualTo("ChargeCalculationRefreshLambda");
    assertThat(capturedInvokeRequest.invocationType()).isEqualByComparingTo(InvocationType.EVENT);
    assertThat(capturedInvokeRequest.payload().asString(Charsets.UTF_8)).isEqualTo("payload");
  }

  @Test
  public void exceptionDuringContextPreparationShouldNotDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    ChargeCalculationRefreshLambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input))
        .willThrow(new AsyncChargeCalculationRefreshLambdaStarterTest.MockedJsonProcessingException(
            "Invalid Json"));

    // when
    lambdaStarter.fireAndForget(CORRELATION_ID, 1);

    // then
    verifyNoInteractions(mockedLambdaClientBuilder);
  }

  private ChargeCalculationRefreshLambdaInput inputObject() {
    return ChargeCalculationRefreshLambdaInput.builder()
        .correlationId(CORRELATION_ID)
        .build();
  }

  private static class MockedJsonProcessingException extends JsonProcessingException {

    protected MockedJsonProcessingException(String msg) {
      super(msg);
    }
  }
}