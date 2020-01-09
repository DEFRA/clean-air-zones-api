package uk.gov.caz.vcc.controller;

import static uk.gov.caz.vcc.controller.VehicleController.VEHICLES_PATH;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javassist.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.vcc.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.dto.VehicleDto;
import uk.gov.caz.vcc.dto.VehicleTypeCazChargesDto;

@RequestMapping(value = VEHICLES_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Api(value = VEHICLES_PATH)
public interface VehicleControllerApiSpec {

  /**
   * Get vehicle details.
   *
   * @param vrn validated string
   * @return Vehicle details about car
   */
  @ApiOperation(value = "${swagger.operations.vehicle.details.description}",
      response = VehicleDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/{vrn}/details")
  ResponseEntity<VehicleDto> details(@PathVariable String vrn);

  /**
   * Get vehicle compliance details.
   *
   * @param vrn validated string
   * @return Vehicle details about car
   */
  @ApiOperation(value = "${swagger.operations.vehicle.compliance.description}",
      response = VehicleDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle compliance details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/{vrn}/compliance")
  ResponseEntity<ComplianceResultsDto> compliance(
      @PathVariable String vrn, @RequestParam("zones") String zones)
      throws NotFoundException;

  /**
   * Get charges for given type.
   *
   * @param type non-null string
   */
  @ApiOperation(value = "${swagger.operations.vehicle.unrecognised.description}",
      response = VehicleTypeCazChargesDto.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "type param missing"),
      @ApiResponse(code = 400, message = "zones parameter malformed"),
      @ApiResponse(code = 200, message = "Vehicle compliance details")})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID",
      required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/unrecognised/{type}/compliance")
  ResponseEntity<VehicleTypeCazChargesDto> unrecognisedVehicle(
      @PathVariable("type") String type, @RequestParam("zones") String zones)
      throws NotFoundException;
}
