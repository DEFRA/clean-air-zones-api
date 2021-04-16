package uk.gov.caz.taxiregister.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;
import uk.gov.caz.taxiregister.util.JsonHelpers;

@Component
@lombok.Value
public class RegisterServicesContext {

  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  VehicleToLicenceConverter licenceConverter;
  TaxiPhvLicenceCsvRepository csvRepository;
  LicencesRegistrationSecuritySentinel securitySentinel;
  EmailSendingValidationErrorsHandler emailSendingValidationErrorsHandler;
  SqsClient sqsClient;
  S3Client s3Client;
  JsonHelpers jsonHelpers;
  String bucket;
  String jobCleanupRequestQueueUrl;
  int maxVehicleRecordCount;
  int jobCleanupRequestDelayInSeconds;
  
  /**
   * Creates an instance of {@link RegisterServicesContext}.
   */
  public RegisterServicesContext(RegisterService registerService,
      RegisterFromCsvExceptionResolver exceptionResolver,
      RegisterJobSupervisor registerJobSupervisor,
      VehicleToLicenceConverter licenceConverter,
      TaxiPhvLicenceCsvRepository csvRepository,
      LicencesRegistrationSecuritySentinel securitySentinel,
      EmailSendingValidationErrorsHandler emailSendingValidationErrorsHandler,
      SqsClient sqsClient,
      S3Client s3Client,
      JsonHelpers jsonHelpers,
      @Value("${application.job-clean-up-request.max-records-count}") int maxVehicleRecordCount,
      @Value("${application.job-clean-up-request.message-visibility-delay-in-seconds}")
      int jobCleanupRequestDelayInSeconds,
      @Value("${application.job-clean-up-request.queue-url}") String jobCleanupRequestQueueUrl,
      @Value("${aws.s3.payload-retention-bucket}") String bucket) {
    this.registerService = registerService;
    this.exceptionResolver = exceptionResolver;
    this.registerJobSupervisor = registerJobSupervisor;
    this.licenceConverter = licenceConverter;
    this.csvRepository = csvRepository;
    this.securitySentinel = securitySentinel;
    this.bucket = bucket;
    this.sqsClient = sqsClient;
    this.s3Client = s3Client;
    this.jsonHelpers = jsonHelpers;
    this.maxVehicleRecordCount = maxVehicleRecordCount;
    this.jobCleanupRequestQueueUrl = jobCleanupRequestQueueUrl;
    this.jobCleanupRequestDelayInSeconds = jobCleanupRequestDelayInSeconds;
    this.emailSendingValidationErrorsHandler = emailSendingValidationErrorsHandler;
  }
}
