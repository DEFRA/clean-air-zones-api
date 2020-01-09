package uk.gov.caz.taxiregister.amazonaws;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.caz.taxiregister.amazonaws.RuntimeExceptionHandlerLambda.EventProcessor;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterServicesContext;

public class RuntimeExceptionHandlerLambdaTest {
  
  private RegisterJobSupervisor mockedRegisterJobSupervisor;
  private SqsClient mockedSqsClient;
  private RegisterServicesContext mockedRegisterServicesContext;
  
  @Captor
  private ArgumentCaptor<List<ValidationError>> validationErrors;
  @Captor
  private ArgumentCaptor<RegisterJobStatus> valueCapture;
  @Captor
  private ArgumentCaptor<Integer> jobIdCaptured;
  @Captor
  private ArgumentCaptor<DeleteMessageRequest> deleteMessageRequestCaptured;
  
  @BeforeEach
  public void init() {
    MockitoAnnotations.initMocks(this);
    mockedRegisterJobSupervisor = mock(RegisterJobSupervisor.class);
    mockedSqsClient = mock(SqsClient.class);
    mockedRegisterServicesContext = mock(RegisterServicesContext.class);
    when(mockedRegisterServicesContext.getRegisterJobSupervisor()).thenReturn(mockedRegisterJobSupervisor);
    when(mockedRegisterServicesContext.getSqsClient()).thenReturn(mockedSqsClient);
  }
  
  @Test
  public void givenValidSnsEventJobWillBeCancelled() throws Exception {
    String eventString = "{" + 
        "    \"Records\": [" + 
        "        {" + 
        "            \"EventSource\": \"aws:sns\"," + 
        "            \"EventVersion\": \"1.0\"," + 
        "            \"EventSubscriptionArn\": \"arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic:1e39cd2a-3f7e-4534-b1f0-6490061dd633\"," + 
        "            \"Sns\": {" + 
        "                \"Type\": \"Notification\"," + 
        "                \"MessageId\": \"b8ca6a4f-91f0-5b4c-bdb0-3fa5edb8b0e4\"," + 
        "                \"TopicArn\": \"arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic\"," + 
        "                \"Subject\": null," + 
        "                \"Message\": \"{\\\"registerJobId\\\":48,\\\"s3Bucket\\\":\\\"s3Bucket\\\",\\\"fileName\\\":\\\"fileName\\\",\\\"correlationId\\\":\\\"correlationId\\\",\\\"action\\\":\\\"action\\\"}\"," + 
        "                \"Timestamp\": \"2019-09-05T02:59:20.437Z\"," + 
        "                \"SignatureVersion\": \"1\"," + 
        "                \"Signature\": \"PvRd3JtfQ6xM0N0dNUMPye1Rh8uXT5CxfZm54yqMYGuDFyqXyQ8E/sirZnQle6yFWAwlKPv8Iv3W3i6jV2iFaIw+j5a7j5GdehNXrMm0rzQz9QIe3KuUhFkkkwPcRZyb6fdT+Nla8uYnBCf9PhT4jg2kcNUmQTfbxCZzIqFfUqRqvgbbaHVOoV29ylZlz0FPXSo8n5ZFoftD0S5bnN7nkzAOj3pl1nO+U9/aV94+sTafpiC9xkPC7cpRNxtBITBZRuXBowKqFRMxcmsek+/+8LFL0z/eOy2ZozKpj9uT81reZ7Ee/gVApaF22rooPrw2sjSnndG/qJOlKgwS3NzFaw==\"," + 
        "                \"SigningCertUrl\": \"https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-6aad65c2f9911b05cd53efda11f913f9.pem\"," + 
        "                \"UnsubscribeUrl\": \"https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic:1e39cd2a-3f7e-4534-b1f0-6490061dd633\"," + 
        "                \"MessageAttributes\": {" + 
        "                    \"RequestID\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"7809dbde-10aa-4014-93ce-48c8954055b5\"\n" + 
        "                    }," + 
        "                    \"ErrorCode\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"200\"" + 
        "                    }," + 
        "                    \"ErrorMessage\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"2019-09-05T02:59:20.251Z 7809dbde-10aa-4014-93ce-48c8954055b5 Task timed out after 30.02 seconds\"" + 
        "                    }" + 
        "                }" + 
        "            }" + 
        "        }" + 
        "    ]" + 
        "}";
        
    doNothing().when(mockedRegisterJobSupervisor).markFailureWithValidationErrors(jobIdCaptured.capture().intValue(),
                                                                                  valueCapture.capture(),
                                                                                  validationErrors.capture());
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterServicesContext);
    eventProcessor.process(eventString);
    assertEquals(new Integer(48), jobIdCaptured.getValue());
    assertEquals(RegisterJobStatus.ABORTED, valueCapture.getValue());
    assertEquals(EventProcessor.LAMBDA_TIMEOUT_EXCEPTION, validationErrors.getValue().get(0).getDetail());
  }
  
  @Test
  public void givenValidCloudWatchLogEventJobWillBeCancelled() throws Exception {
    String eventString = "{" + 
        "\"awslogs\": {" + 
        "        \"data\": \"H4sIAAAAAAAAAM2TW2/aMBTHv0pk7ZEE32PnjW0EMdEiAeoLoMkkBmXLhTkJbYX47jvJxspaVXtdFMnH/3Px8S8nZ1TYujYHu3o+WhShz6PV6OvdeLkcTcZogKrH0jqQMVGMYYkplxzkvDpMXNUewTM0j/UwN8UuNcOFPWR1Y92n+hS7qliyuC2TJqvKXynLxllTQA7FRA8xvGK4/jAbrcbL1ZboXSj2VDORSm4F0VjRkBDLLKYk5AmUqNtdnbjs2FWMsxwOqlG0Rt/Mj9YvssRVtXWnLLG1P2+b+f7OFpV7Hj8l9ibDJ7N4MV/M2MOEPCi07Rsbn2zZdLXOKEuhP8a10JoTJghVlApNMSehliEnWComsQiZlpJxAQ9WAkvKsMICemwy4NmYAtAQIUMpQq0UIXhw5dzBFN3lOwge5hFjEVYBhHje9D6eex7xfN/31t7LU5is3HptcAiSoAlM8IbzrOd/DY+82/s7VznvvYzzBrnfri/VbppuUMTVYINq9rFNvtsG9rcb8Oyz3N6bwvaelw14kso5m5uOdV/ojQIxph+H3nk1L5tuPOxT40CxaZzZPIVvcUbX4ekOAGrvXAFyyV9MQaCdcEMWJAZShxdMDiaBVcAKpMGSYL3CDWoI6j+ZQ5yCuAjWa7/T8tg2oPwPbKGtPz8AtPR6LtDlsr38BFns9Q0DBAAA\"" + 
        "  }" + 
        "}";
    doNothing().when(mockedRegisterJobSupervisor).markFailureWithValidationErrors(jobIdCaptured.capture().intValue(),
                                                                                  valueCapture.capture(),
                                                                                  validationErrors.capture());
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterServicesContext);
    eventProcessor.process(eventString);
    assertEquals(new Integer(48), jobIdCaptured.getValue());
    assertEquals(RegisterJobStatus.ABORTED, valueCapture.getValue());
    assertEquals(EventProcessor.LAMBDA_OUTOFMEMORY_EXCEPTION, validationErrors.getValue().get(0).getDetail());
  }
  
  @Test
  public void givenValidSQSEventJobWillBeCancelled() throws Exception {
    String eventString = "{" + 
        "  \"Records\": [" + 
        "    {" + 
        "      \"body\": \"{\\\"registerJobId\\\":48}\", " + 
        "      \"receiptHandle\": \"MessageReceiptHandle\", " + 
        "      \"md5OfBody\": \"7b270e59b47ff90a553787216d55d91d\", " + 
        "      \"eventSourceARN\": \"arn:aws:sqs:eu-west-2:123456789012:job-clean-up-request\", " + 
        "      \"eventSource\": \"aws:sqs\", " + 
        "      \"awsRegion\": \"eu-west-2\", " + 
        "      \"messageId\": \"19dd0b57-b21e-4ac1-bd88-01bbb068cb78\", " + 
        "      \"attributes\": {" + 
        "        \"ApproximateFirstReceiveTimestamp\": \"1523232000001\", " + 
        "        \"SenderId\": \"123456789012\", " + 
        "        \"ApproximateReceiveCount\": \"1\", " + 
        "        \"SentTimestamp\": \"1523232000000\"" + 
        "      }, " + 
        "      \"messageAttributes\": {}" + 
        "    }" + 
        "  ]" + 
        "}" + 
        "";
    
    RegisterJob job = mock(RegisterJob.class);
    when(job.getStatus()).thenReturn(RegisterJobStatus.RUNNING);
    when(job.getId()).thenReturn(48);
    when(mockedRegisterJobSupervisor.findJobById(48)).thenReturn(Optional.of(job));
    doNothing().when(mockedRegisterJobSupervisor).markFailureWithValidationErrors(jobIdCaptured.capture().intValue(),
        valueCapture.capture(),
        validationErrors.capture());
    
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterServicesContext);
    eventProcessor.process(eventString);
    assertEquals(new Integer(48), jobIdCaptured.getValue());
    assertEquals(RegisterJobStatus.ABORTED, valueCapture.getValue());
    assertEquals(EventProcessor.LAMBDA_TIMEOUT_EXCEPTION, validationErrors.getValue().get(0).getDetail());
    verify(mockedSqsClient,times(1)).deleteMessage(deleteMessageRequestCaptured.capture());
    DeleteMessageRequest request = deleteMessageRequestCaptured.getValue();
    assertEquals("MessageReceiptHandle", request.receiptHandle());
  }
  
  @Test
  public void givenInvalidEventWillThrowException() throws Exception {
    String invalidEventString = "Invalid Event";
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterServicesContext);
    assertThrows(Exception.class, () -> eventProcessor.process(invalidEventString));
  }
}
