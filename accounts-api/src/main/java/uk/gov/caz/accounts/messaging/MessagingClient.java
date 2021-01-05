package uk.gov.caz.accounts.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

/**
 * A wrapper class for an external queueing system with ability to publish mesages to the queue.
 */
@Component
@Slf4j
public class MessagingClient {

  private final String newQueueName;
  private final AmazonSQS client;
  private final ObjectMapper objectMapper;

  /**
   * A dependency injection constructor for MessagingClient.
   *
   * @param newQueueName the name of the "new" queue
   * @param client the client to interface with Amazon SQS (external messaging provider)
   * @param objectMapper a mapper to convert SendEmailRequest objects to strings
   */
  public MessagingClient(@Value("${services.sqs.new-queue-name}") String newQueueName,
      AmazonSQS client, ObjectMapper objectMapper) {
    this.newQueueName = newQueueName;
    this.client = client;
    this.objectMapper = objectMapper;
  }

  /**
   * A method to publish a message to a queue.
   *
   * @param message the message to be published
   * @throws EmailSerializationException thrown if the message is unable to be processed into a
   *     string
   */
  public void publishMessage(SendEmailRequest message) {
    SendMessageRequest sendMessageRequest = new SendMessageRequest();
    UUID messageDeduplicationId = UUID.randomUUID();

    try {
      sendMessageRequest.setQueueUrl(client.getQueueUrl(newQueueName).getQueueUrl());
      sendMessageRequest.setMessageBody(objectMapper.writeValueAsString(message));
      sendMessageRequest.setMessageGroupId(UUID.randomUUID().toString());
      sendMessageRequest.setMessageDeduplicationId(messageDeduplicationId.toString());
      sendMessageRequest.putCustomRequestHeader("contentType", "application/json");
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize message data.");
      throw new EmailSerializationException("Could not serialize a message.");
    }

    log.info("Sending email message object to SQS queue {} with de-duplication ID: {}",
        newQueueName, messageDeduplicationId);
    client.sendMessage(sendMessageRequest);
  }
}
