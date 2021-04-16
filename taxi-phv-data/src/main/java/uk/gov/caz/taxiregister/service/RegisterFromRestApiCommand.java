package uk.gov.caz.taxiregister.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.util.JsonHelpers;

@Slf4j
public class RegisterFromRestApiCommand extends AbstractRegisterCommand {

  private final UUID uploaderId;
  private final List<VehicleDto> licences;
  private final S3Client s3Client;
  private final JsonHelpers jsonHelpers;
  private SqsClient sqsClient;
  private String bucket;
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
    this.bucket = registerServicesContext.getBucket();
    this.jsonHelpers = registerServicesContext.getJsonHelpers();
    this.s3Client = registerServicesContext.getS3Client();
    this.sqsClient = registerServicesContext.getSqsClient();
    this.queueUrl = registerServicesContext.getJobCleanupRequestQueueUrl();
    this.maxVehicleRecordCount = registerServicesContext.getMaxVehicleRecordCount();
    this.messageVisibilityDelayInSeconds =
        registerServicesContext.getJobCleanupRequestDelayInSeconds();
  }

  @Override
  void beforeExecute() {
    Assert.notNull(queueUrl, "Please set "
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
  void onBeforeMarkJobFailed(RegisterJobStatus jobStatus, List<ValidationError> validationErrors) {
    // nothing to be done here when registering from REST API
  }

  @Override
  void afterMarkJobFinished(ConversionResults conversionResults) {
    try {
      s3Client.putObject(prepareRequestObject(), prepareRequestBody(conversionResults));
    } catch (Exception e) {
      log.error("An error occurred during upload json file to s3. ", e);
    }
  }

  private PutObjectRequest prepareRequestObject() {
    return PutObjectRequest.builder()
        .bucket(bucket)
        .key(prepareFileName())
        .contentType(APPLICATION_JSON_VALUE)
        .build();
  }

  private RequestBody prepareRequestBody(ConversionResults conversionResults) {
    String licences = jsonHelpers.toPrettyJson(conversionResults.getLicences());
    return RequestBody.fromBytes(licences.getBytes());
  }

  private String prepareFileName() {
    return getCorrelationId() + " " + LocalDateTime.now();
  }
}