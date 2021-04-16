package uk.gov.caz.vcc.controller.v2;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.caz.vcc.dto.ErrorsResponse;
import uk.gov.caz.vcc.dto.VehicleEntrantSaveDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsDtoV2;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.dto.VehicleResultsDto;
import uk.gov.caz.vcc.dto.validation.ValidationError;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

/**
 * Rest Controller with endpoints related to /v2 vehicle entrant requests.
 */
@RestController
public class VehicleEntrantsControllerV2 implements VehicleEntrantsControllerV2ApiSpec {

  public static final String VEHICLE_ENTRANT_PATH_V2 = "v2/vehicle-entrants";

  public static final String CAZ_ID = "x-api-key";

  public static final String TIMESTAMP = "timestamp";

  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern(DATE_TIME_FORMAT);

  private final VehicleEntrantsService vehicleEntrantsService;
  private final int maxErrorsCount;

  public VehicleEntrantsControllerV2(VehicleEntrantsService vehicleEntrantsService,
      @Value("${application.validation.vehicle-entrants.max-errors-count}") int maxErrorsCount) {
    this.vehicleEntrantsService = vehicleEntrantsService;
    this.maxErrorsCount = maxErrorsCount;
  }

  @Override
  public ResponseEntity vehicleEntrants(@RequestBody VehicleEntrantsDtoV2 vehicleEntrants,
      @RequestHeader(X_CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(CAZ_ID) String cazId, @RequestHeader(TIMESTAMP) String timestamp) {
    List<ValidationError> validationErrors = checkPreconditions(vehicleEntrants, timestamp);

    if (!validationErrors.isEmpty()) {
      return ResponseEntity.badRequest().body(getValidationErrorsResponse(validationErrors));
    }
    
    validationErrors = vehicleEntrants.validate();
    if (!validationErrors.isEmpty()) {
      return ResponseEntity.badRequest().body(getValidationErrorsResponse(validationErrors));
    }

    return ResponseEntity.ok(new VehicleResultsDto(
        saveVehicleEntrantsAndGetDetails(vehicleEntrants, correlationId, cazId)));
  }

  private List<ValidationError> checkPreconditions(
      VehicleEntrantsDtoV2 vehicleEntrants, String timestamp) {

    List<ValidationError> validationErrors = new LinkedList<>();

    try {
      checkTimestampHeader(timestamp);
      checkVehicleEntrants(vehicleEntrants);
    } catch (TimestampHeaderValidationException preCheckException) {
      ValidationError error =
          new ValidationError(TimestampHeaderValidationException.title,
              preCheckException.getMessage(), null);
      validationErrors.add(error);
    } catch (VehicleEntrantsListNullException preCheckException) {
      ValidationError error =
          new ValidationError(VehicleEntrantsListNullException.title,
              preCheckException.getMessage(), null);
      validationErrors.add(error);
    }

    return validationErrors;
  }

  private void checkTimestampHeader(String timestamp) {
    try {
      LocalDateTime.parse(timestamp, DATE_TIME_FORMATTER);
    } catch (Exception e) {
      throw new TimestampHeaderValidationException(timestamp);
    }
  }

  private void checkVehicleEntrants(VehicleEntrantsDtoV2 vehicleEntrants) {
    if (vehicleEntrants.getVehicleEntrants() == null) {
      throw new VehicleEntrantsListNullException();
    }
  }

  private ErrorsResponse getValidationErrorsResponse(List<ValidationError> validationErrors) {
    List<ValidationError> errors =
        validationErrors.stream().limit(maxErrorsCount).collect(Collectors.toList());
    return ErrorsResponse.from(errors);
  }

  private List<VehicleResultDto> saveVehicleEntrantsAndGetDetails(
      VehicleEntrantsDtoV2 vehicleEntrants, String correlationId, String cazId) {

    List<VehicleEntrantSaveDto> vehicleEntrantSaveDtos = vehicleEntrants
        .getVehicleEntrants()
        .stream()
        .map((vehicleEntrantDto) -> VehicleEntrantSaveDto
            .from(vehicleEntrantDto, DATE_TIME_FORMATTER))
        .collect(Collectors.toList());

    return vehicleEntrantsService.save(new VehicleEntrantsSaveRequestDto(UUID.fromString(cazId),
        correlationId, vehicleEntrantSaveDtos));
  }

  static class VehicleEntrantsListNullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    static String title = "Vehicle Entrants not present";
    
    public VehicleEntrantsListNullException() {
      super("List of vehicle entrants cannot not be null.");
    }
  }

  static class TimestampHeaderValidationException extends RuntimeException {

    private static final long serialVersionUID = -8532676208088492955L;

    static String title = "Invalid Timestamp request header value";
    
    public TimestampHeaderValidationException(String timestamp) {
      super(String.format(
          "Invalid Timestamp header for value: %s. The expected format is 2020-05-22T13:26:00Z",
          timestamp));
    }
  }
}
