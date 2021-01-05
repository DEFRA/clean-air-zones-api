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
import uk.gov.caz.accounts.dto.ChargeCalculationLambdaInput;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationStarter;
import uk.gov.caz.accounts.util.MockedLambdaClient;

class AsyncChargeCalculationLambdaStarterTest {

  private static final String LAMBDA_NAME = "ChargeCalculationLambda";
  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final UUID CORRELATION_ID = UUID.randomUUID();
  private static final Integer JOB_ID = 1;

  private ObjectMapper mockedObjectMapper;
  private LambdaClientBuilder mockedLambdaClientBuilder;
  private AsyncChargeCalculationStarter lambdaStarter;

  @BeforeEach
  public void init() {
    mockedObjectMapper = mock(ObjectMapper.class);
    mockedLambdaClientBuilder = mock(LambdaClientBuilder.class);
    lambdaStarter = new AsyncChargeCalculationLambdaStarter(mockedObjectMapper,
        mockedLambdaClientBuilder, LAMBDA_NAME);
  }

  @Test
  public void startingLambdaShouldPrepareContextAndDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    ChargeCalculationLambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input)).willReturn("payload");

    MockedLambdaClient mockedLambdaClient = new MockedLambdaClient();
    given(mockedLambdaClientBuilder.build()).willReturn(mockedLambdaClient);

    // when
    lambdaStarter.fireAndForget(ACCOUNT_ID, JOB_ID, CORRELATION_ID, 1,
        true);

    // then
    InvokeRequest capturedInvokeRequest = mockedLambdaClient.getCapturedInvokeRequest();
    assertThat(capturedInvokeRequest).isNotNull();
    assertThat(capturedInvokeRequest.functionName()).isEqualTo("ChargeCalculationLambda");
    assertThat(capturedInvokeRequest.invocationType()).isEqualByComparingTo(InvocationType.EVENT);
    assertThat(capturedInvokeRequest.payload().asString(Charsets.UTF_8)).isEqualTo("payload");
  }

  @Test
  public void exceptionDuringContextPreparationShouldNotDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    ChargeCalculationLambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input))
        .willThrow(new MockedJsonProcessingException("Invalid Json"));

    // when
    lambdaStarter.fireAndForget(ACCOUNT_ID, JOB_ID, CORRELATION_ID,1,
        true);

    // then
    verifyNoInteractions(mockedLambdaClientBuilder);
  }

  private ChargeCalculationLambdaInput inputObject() {
    return ChargeCalculationLambdaInput.builder()
        .accountId(ACCOUNT_ID)
        .jobId(JOB_ID)
        .correlationId(CORRELATION_ID)
        .shouldSendEmailsUponSuccessfulJobCompletion(true)
        .build();
  }

  private static class MockedJsonProcessingException extends JsonProcessingException {

    protected MockedJsonProcessingException(String msg) {
      super(msg);
    }
  }
}