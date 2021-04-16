package uk.gov.caz.vcc.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.vcc.dto.VehicleEntrantReportingRequest;

/**
 * A wrapper class for an external queuing system with the ability to publish messages to the queue.
 *
 */
@Component
@Slf4j
public class MessagingClient {

  private final String queueName;
  private final AmazonSQS client;
  private final ObjectMapper objectMapper;

  /**
   * A dependency injection constructor for MessagingClient.
   * 
   * @param queueName the name of the queue
   * @param client the client to interface with Amazon SQS (external messaging provider)
   * @param objectMapper a mapper to convert SendEmailRequest objects to strings
   */
  public MessagingClient(@Value("${services.sqs.reporting-data-queue-name}") String queueName, 
      AmazonSQS client, ObjectMapper objectMapper) {
    this.queueName = queueName;
    this.client = client;
    this.objectMapper = objectMapper;
  }

  /**
   * A method to publish a message to a queue.
   * 
   * @param message the message to be published
   * @throws JsonProcessingException thrown if the message is unable to be processed into a string
   */
  public void publishMessage(List<VehicleEntrantReportingRequest> message) {
    SendMessageRequest sendMessageRequest = new SendMessageRequest();

    try {
      sendMessageRequest.setQueueUrl(client.getQueueUrl(queueName).getQueueUrl());
      sendMessageRequest.setMessageBody(objectMapper.writeValueAsString(message));
      sendMessageRequest.setMessageGroupId(UUID.randomUUID().toString());
      sendMessageRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
      sendMessageRequest.putCustomRequestHeader("contentType", "application/json");
      client.sendMessage(sendMessageRequest);
    } catch (JsonProcessingException e) {
      log.error("Failed to publish message");
    }
    
    log.info("Message successfully published");
  }

}
