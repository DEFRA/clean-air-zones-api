package uk.gov.caz.vcc.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SqsTestUtility {
  
  private final AmazonSQS sqsClient;
  
  public void createQueue(String queueName) {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }
  
  public List<Message> receiveSqsMessages(String queueName) {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(queueName);
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
        queueUrlResult.getQueueUrl());
    receiveMessageRequest.withMaxNumberOfMessages(10);
    ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
    return receiveMessageResult.getMessages();
  }

  public void deleteQueue(String queueName) {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(queueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());    
  }

}
