package uk.gov.caz.vcc.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.vcc.dto.ErrorsResponse;
import uk.gov.caz.vcc.dto.VehicleEntrantsDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.dto.VehicleResultsDto;
import uk.gov.caz.vcc.dto.validation.ValidationError;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

/**
 * Rest Controller with endpoints related to vehicle entrants.
 */
@RestController
public class VehicleEntrantsController implements VehicleEntrantsControllerApiSpec {

  public static final String VEHICLE_ENTRANT_PATH = "/v1/vehicle-entrants";

  public static final String CAZ_ID = "x-api-key";

  private final VehicleEntrantsService vehicleEntrantsService;
  private final int maxErrorsCount;

  @Autowired
  public VehicleEntrantsController(VehicleEntrantsService vehicleEntrantsService,
      @Value("${application.validation.vehicle-entrants.max-errors-count}") int maxErrorsCount) {
    this.vehicleEntrantsService = vehicleEntrantsService;
    this.maxErrorsCount = maxErrorsCount;
  }

  @Override
  public ResponseEntity vehicleEntrant(
      @RequestBody VehicleEntrantsDto vehicleEntrants,
      @RequestHeader(X_CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(CAZ_ID) String cazId) {
    checkPreconditions(vehicleEntrants);

    List<ValidationError> validationErrors = vehicleEntrants.validate();
    if (!validationErrors.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(getValidationErrorsResponse(validationErrors));
    }

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(new VehicleResultsDto(
            saveVehicleEntrantsAndGetDetails(vehicleEntrants, correlationId, cazId)));
  }

  private void checkPreconditions(VehicleEntrantsDto vehicleEntrants) {
    if (vehicleEntrants.getVehicleEntrants() == null) {
      throw new VehicleEntrantsListNullException();
    }
  }

  private List<VehicleResultDto> saveVehicleEntrantsAndGetDetails(
      @RequestBody VehicleEntrantsDto vehicleEntrants,
      @RequestHeader(X_CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(CAZ_ID) String cazId) {
    return vehicleEntrantsService.save(new VehicleEntrantsSaveRequestDto(
        UUID.fromString(cazId),
        correlationId,
        vehicleEntrants.getVehicleEntrants()
    ));
  }

  private ErrorsResponse getValidationErrorsResponse(List<ValidationError> validationErrors) {
    List<ValidationError> errors = validationErrors.stream()
        .limit(maxErrorsCount)
        .collect(Collectors.toList());
    return ErrorsResponse.from(errors);
  }

  @ResponseStatus(BAD_REQUEST)
  static class VehicleEntrantsListNullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehicleEntrantsListNullException() {
      super("List of vehicle entrants cannot not be null.");
    }
  }
}