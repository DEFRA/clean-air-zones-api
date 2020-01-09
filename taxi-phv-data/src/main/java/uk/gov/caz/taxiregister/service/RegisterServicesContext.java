package uk.gov.caz.taxiregister.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;

@Component
@lombok.Value
public class RegisterServicesContext {

  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  VehicleToLicenceConverter licenceConverter;
  TaxiPhvLicenceCsvRepository csvRepository;
  LicencesRegistrationSecuritySentinel securitySentinel;
  int maxValidationErrorCount;
  SqsClient sqsClient;
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
      SqsClient sqsClient,
      @Value("${application.validation.max-errors-count}") int maxValidationErrorCount,
      @Value("${application.job-clean-up-request.max-records-count}") int maxVehicleRecordCount,
      @Value("${application.job-clean-up-request.message-visibility-delay-in-seconds}")
      int jobCleanupRequestDelayInSeconds,
      @Value("${application.job-clean-up-request.queue-url}") String jobCleanupRequestQueueUrl) {
    this.registerService = registerService;
    this.exceptionResolver = exceptionResolver;
    this.registerJobSupervisor = registerJobSupervisor;
    this.licenceConverter = licenceConverter;
    this.csvRepository = csvRepository;
    this.securitySentinel = securitySentinel;
    this.sqsClient = sqsClient;
    this.maxValidationErrorCount = maxValidationErrorCount;
    this.maxVehicleRecordCount = maxVehicleRecordCount;
    this.jobCleanupRequestQueueUrl = jobCleanupRequestQueueUrl;
    this.jobCleanupRequestDelayInSeconds = jobCleanupRequestDelayInSeconds;
  }
}
