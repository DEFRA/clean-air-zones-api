package uk.gov.caz.accounts.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

@ExtendWith(MockitoExtension.class)
class MessagingClientTest {

  @Mock
  AmazonSQS client;

  MessagingClient messagingClient;

  SendEmailRequest sendEmailRequest;

  ObjectMapper objectMapper = mock(ObjectMapper.class);

  private static final String ANY_EMAIL = "test@email.com";

  @BeforeEach
  void init() {
    messagingClient = new MessagingClient("testQueue", client, objectMapper);
    sendEmailRequest = SendEmailRequest.builder()
        .emailAddress(ANY_EMAIL)
        .build();
  }

  @Test
  void canPublishMessage() throws JsonProcessingException {
    mockValidObjectMapperSerialization();
    GetQueueUrlResult result = new GetQueueUrlResult();
    result.setQueueUrl("newUrl");
    when(client.getQueueUrl("testQueue")).thenReturn(result);

    messagingClient.publishMessage(sendEmailRequest);

    Mockito.verify(client, times(1)).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  void throwsEmailSerializationException() throws JsonProcessingException {
    mockInvalidObjectMapperSerialization();
    GetQueueUrlResult result = new GetQueueUrlResult();
    result.setQueueUrl("newUrl");
    when(client.getQueueUrl("testQueue")).thenReturn(result);

    Throwable throwable = catchThrowable(() -> messagingClient.publishMessage(sendEmailRequest));

    assertThat(throwable).isInstanceOf(EmailSerializationException.class)
        .hasMessage("Could not serialize a message.");
  }

  private void mockValidObjectMapperSerialization() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any())).thenReturn(ANY_EMAIL);
  }

  private void mockInvalidObjectMapperSerialization() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
  }
}
