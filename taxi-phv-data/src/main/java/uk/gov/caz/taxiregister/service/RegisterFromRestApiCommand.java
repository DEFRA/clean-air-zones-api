package uk.gov.caz.taxiregister.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

@Slf4j
public class RegisterFromRestApiCommand extends AbstractRegisterCommand {

  private final UUID uploaderId;
  private final List<VehicleDto> licences;
  private SqsClient sqsClient;
  private String queueUrl;
  private int maxVehicleRecordCount;
  private int messageVisibilityDelayInSeconds;
  
  /**
   * Creates an instance of {@link RegisterFromRestApiCommand}.
   */
  public RegisterFromRestApiCommand(List<VehicleDto> licences, UUID uploaderId, int registerJobId,
      RegisterServicesContext registerServicesContext, String correlationId) {
    super(registerServicesContext, registerJobId, correlationId);
    this.uploaderId = uploaderId;
    this.licences = licences;
    this.sqsClient = registerServicesContext.getSqsClient();
    this.queueUrl = registerServicesContext.getJobCleanupRequestQueueUrl();
    this.maxVehicleRecordCount = registerServicesContext.getMaxVehicleRecordCount();
    this.messageVisibilityDelayInSeconds = 
        registerServicesContext.getJobCleanupRequestDelayInSeconds();
  }

  @Override
  void beforeExecute() {
    Assert.notNull(queueUrl,"Please set "
        + "application.job-clean-up-request.queue-url property value");
    if (shouldCreateJobCleanUpRequest()) {
      String messageBody = String.format("{\"registerJobId\": %d}", getRegisterJobId());
      sqsClient.sendMessage(SendMessageRequest.builder()
          .queueUrl(queueUrl)
          .messageBody(messageBody)
          .delaySeconds(messageVisibilityDelayInSeconds)
          .build());
    }
  }

  boolean shouldCreateJobCleanUpRequest() {
    return getLicencesToRegister().size() > maxVehicleRecordCount;
  }
  
  @Override
  UUID getUploaderId() {
    return uploaderId;
  }

  @Override
  List<VehicleDto> getLicencesToRegister() {
    return licences;
  }

  @Override
  List<ValidationError> getLicencesParseValidationErrors() {
    return Collections.emptyList();
  }

  @Override
  boolean shouldMarkJobFailed() {
    return true;
  }

  @Override
  void onBeforeMarkJobFailed() {
    // nothing to be done here when registering from REST API
  }
}
