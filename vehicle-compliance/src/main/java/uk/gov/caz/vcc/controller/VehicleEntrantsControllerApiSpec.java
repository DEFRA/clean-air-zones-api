package uk.gov.caz.vcc.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.vcc.controller.VehicleEntrantsController.CAZ_ID;
import static uk.gov.caz.vcc.controller.VehicleEntrantsController.VEHICLE_ENTRANT_PATH;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.vcc.dto.VehicleEntrantsDto;
import uk.gov.caz.vcc.dto.VehicleResultsDto;

/**
 * Interface with swagger documentation for VehicleEntrantsController.
 */
@RequestMapping(value = VEHICLE_ENTRANT_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE)
@Api(value = VEHICLE_ENTRANT_PATH)
public interface VehicleEntrantsControllerApiSpec {

  /**
   * This method allows the submission of requests that describes one or more vehicle entrants to a
   * CAZ for the purpose of determining: Whether a vehicle is compliant with the CAZ Framework. If a
   * vehicle is not compliant with the CAZ Framework, whether it is exempt from being charged based
   * on national exemptions/white-lists. If a vehicle is not exempt, whether the vehicle should be
   * charged and the tariff that applies based on how a given LA is operating their CAZ scheme.
   *
   * @param vehicleEntrants {@link VehicleEntrantsDto}
   * @return Vehicle details about car for given VRN
   */
  @ApiOperation(value = "${swagger.operations.vehicleResults.description}",
      response = VehicleResultsDto.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle Results"),})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = CAZ_ID,
          required = true,
          value = "API key used to access the service",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization",
          required = true,
          value = "OAuth 2.0 authorisation token.",
          paramType = "header"),
      @ApiImplicitParam(name = "timestamp",
          required = true,
          value =
              "ISO 8601 formatted datetime string indicating the time that the request was made.",
          paramType = "header"),
  })
  @PostMapping
  ResponseEntity vehicleEntrant(
      @RequestBody VehicleEntrantsDto vehicleEntrants,
      @RequestHeader(X_CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(CAZ_ID) String cazId);
}