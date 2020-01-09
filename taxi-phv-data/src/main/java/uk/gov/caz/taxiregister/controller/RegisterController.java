package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.taxiregister.controller.Constants.API_KEY_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus.FINISHED_SUCCESS;

import com.google.common.base.Stopwatch;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.controller.exception.InvalidUploaderIdFormatException;
import uk.gov.caz.taxiregister.controller.exception.PayloadValidationException;
import uk.gov.caz.taxiregister.dto.ErrorResponse;
import uk.gov.caz.taxiregister.dto.ErrorsResponse;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;

@RestController
@Slf4j
public class RegisterController implements RegisterControllerApiSpec {

  public static final String PATH = "/v1/scheme-management";
  static final String INVALID_UPLOADER_ID_ERROR_MESSAGE = "Invalid format of uploader-id, "
      + "expected: UUID";
  static final String NULL_VEHICLE_DETAILS_ERROR_MESSAGE = "'vehicleDetails' cannot be null";
  private static final String REGISTER_JOB_NAME_SUFFIX = "";

  private final RegisterJobSupervisor registerJobSupervisor;
  private final SourceAwareRegisterService registerService;
  private final int maxLicencesCount;

  /**
   * Creates an instance of {@link RegisterController}.
   */
  public RegisterController(RegisterJobSupervisor registerJobSupervisor,
      SourceAwareRegisterService registerService,
      @Value("${api.max-licences-count}") int maxLicencesCount) {
    this.registerJobSupervisor = registerJobSupervisor;
    this.registerService = registerService;
    this.maxLicencesCount = maxLicencesCount;
    log.info("Set {} as the maximum number of vehicles handled by REST API", maxLicencesCount);
  }

  @Override
  public ResponseEntity register(@RequestBody Vehicles vehicles,
      @RequestHeader(CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(API_KEY_HEADER) String apiKey) {
    log.info("Controller start processing request at {}ms", System.currentTimeMillis());
    Stopwatch timer = Stopwatch.createStarted();
    UUID uploaderId = extractUploaderId(apiKey);
    checkPreconditions(vehicles);

    RegisterJobName registerJobName = registerJobSupervisor.start(
        StartParams.builder()
            .correlationId(correlationId)
            .uploaderId(uploaderId)
            .registerJobNameSuffix(REGISTER_JOB_NAME_SUFFIX)
            .registerJobTrigger(RegisterJobTrigger.API_CALL)
            .registerJobInvoker(
                registerJobId -> registerService.register(vehicles.getVehicleDetails(),
                    uploaderId, registerJobId, correlationId))
            .build());

    log.info("Register method took {}ms", timer.stop().elapsed(TimeUnit.MILLISECONDS));
    return registerJobSupervisor.findJobWithName(registerJobName)
        .map(registerJob -> toResponseEntity(registerJob, correlationId))
        .orElseThrow(() -> new IllegalStateException(
            "Unable to fetch register job instance for job name: " + registerJobName.getValue()));
  }

  private UUID extractUploaderId(String apiKey) {
    try {
      return UUID.fromString(apiKey);
    } catch (IllegalArgumentException e) {
      throw new InvalidUploaderIdFormatException(INVALID_UPLOADER_ID_ERROR_MESSAGE);
    }
  }

  private ResponseEntity toResponseEntity(RegisterJob registerJob, String correlationId) {
    if (registerJob.getStatus() == FINISHED_SUCCESS) {
      return createSuccessRegisterResponse(correlationId);
    }
    return createFailureRegisterResponse(correlationId, registerJob);
  }

  private ResponseEntity<ErrorsResponse> createFailureRegisterResponse(String correlationId,
      RegisterJob registerJob) {
    List<ErrorResponse> errors = registerJob.getErrors()
        .stream()
        .map(ErrorResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.badRequest()
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(ErrorsResponse.from(errors));
  }

  private ResponseEntity<Map<String, String>> createSuccessRegisterResponse(String correlationId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(Collections.singletonMap("detail", "Register updated successfully."));
  }

  private void checkPreconditions(Vehicles vehicles) {
    checkNotNullPrecondition(vehicles.getVehicleDetails());
    checkMaxLicencesCountPrecondition(vehicles);
  }

  private void checkNotNullPrecondition(List<VehicleDto> vehicleDetails) {
    if (vehicleDetails == null) {
      throw new PayloadValidationException(NULL_VEHICLE_DETAILS_ERROR_MESSAGE);
    }
  }

  private void checkMaxLicencesCountPrecondition(Vehicles vehicles) {
    if (vehicles.getVehicleDetails().size() > maxLicencesCount) {
      throw new PayloadValidationException(
          String.format("Max number of vehicles exceeded. Expected: up to %d, actual: %d. "
                  + "Please contact the system administrator for further information.",
              maxLicencesCount, vehicles.getVehicleDetails().size())
      );
    }
  }
}
