package uk.gov.caz.accounts.amazonaws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.caz.accounts.util.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_CORRELATION_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.accounts.util.MockedLambdaClient;

class AsyncLambdaBackgroundJobStarterTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String CSV_FILE = "fileName";
  private static final String LAMBDA_NAME = "RegisterCsvFromS3Function";

  private ObjectMapper mockedObjectMapper;
  private LambdaClientBuilder mockedLambdaClientBuilder;
  private AsyncLambdaBackgroundJobStarter lambdaJobStarter;

  @BeforeEach
  public void init() {
    mockedObjectMapper = mock(ObjectMapper.class);
    mockedLambdaClientBuilder = mock(LambdaClientBuilder.class);
    lambdaJobStarter = new AsyncLambdaBackgroundJobStarter(mockedObjectMapper,
        mockedLambdaClientBuilder, LAMBDA_NAME);
  }

  @Test
  public void startingLambdaShouldPrepareContextAndDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    RegisterCsvFromS3LambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input)).willReturn("payload");

    MockedLambdaClient mockedLambdaClient = new MockedLambdaClient();
    given(mockedLambdaClientBuilder.build()).willReturn(mockedLambdaClient);

    // when
    lambdaJobStarter
        .fireAndForgetRegisterCsvFromS3Job(S3_REGISTER_JOB_ID, S3_BUCKET, CSV_FILE,
            TYPICAL_CORRELATION_ID, true);

    // then
    InvokeRequest capturedInvokeRequest = mockedLambdaClient.getCapturedInvokeRequest();
    assertThat(capturedInvokeRequest).isNotNull();
    assertThat(capturedInvokeRequest.functionName()).isEqualTo("RegisterCsvFromS3Function");
    assertThat(capturedInvokeRequest.invocationType()).isEqualByComparingTo(InvocationType.EVENT);
    assertThat(capturedInvokeRequest.payload().asString(Charsets.UTF_8)).isEqualTo("payload");
  }

  @Test
  public void exceptionDuringContextPreparationShouldNotDelegateToLambdaClientImplementation()
      throws JsonProcessingException {
    // given
    RegisterCsvFromS3LambdaInput input = inputObject();
    given(mockedObjectMapper.writeValueAsString(input))
        .willThrow(new MockedJsonProcessingException("Invalid Json"));

    // when
    lambdaJobStarter.fireAndForgetRegisterCsvFromS3Job(S3_REGISTER_JOB_ID, S3_BUCKET, CSV_FILE,
        TYPICAL_CORRELATION_ID, true);

    // then
    verifyNoInteractions(mockedLambdaClientBuilder);
  }

  private RegisterCsvFromS3LambdaInput inputObject() {
    return RegisterCsvFromS3LambdaInput.builder()
        .s3Bucket(S3_BUCKET)
        .registerJobId(S3_REGISTER_JOB_ID)
        .fileName(CSV_FILE)
        .correlationId(TYPICAL_CORRELATION_ID)
        .shouldSendEmailsUponSuccessfulChargeCalculation(true)
        .build();
  }

  private static class MockedJsonProcessingException extends JsonProcessingException {

    protected MockedJsonProcessingException(String msg) {
      super(msg);
    }
  }
}