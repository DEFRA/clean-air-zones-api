package uk.gov.caz.vcc.controller;

import static uk.gov.caz.vcc.controller.VehicleController.VEHICLES_PATH;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javassist.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.vcc.dto.RegisterDetailsDto;
import uk.gov.caz.vcc.dto.VehicleFromDvlaDto;

/**
 * Interface with swagger documentation for VehicleController.
 */
@RequestMapping(value = VEHICLES_PATH,
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
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
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
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
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invalid vrn"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle compliance details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/{vrn}/compliance")
  ResponseEntity<ComplianceResultsDto> compliance(
      @PathVariable String vrn,
      @RequestParam(value = "zones", required = false) String zones)
      throws NotFoundException;

  /**
   * Get charges for given type.
   *
   * @param type non-null string
   */
  @ApiOperation(value = "${swagger.operations.vehicle.unrecognised.description}",
      response = VehicleTypeCazChargesDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "type param missing"),
      @ApiResponse(code = 400, message = "zones parameter malformed"),
      @ApiResponse(code = 200, message = "Vehicle compliance details")})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/unrecognised/{type}/compliance")
  ResponseEntity<VehicleTypeCazChargesDto> unrecognisedVehicle(@PathVariable("type") String type,
      @RequestParam("zones") String zones) throws NotFoundException;

  /**
   * Get compliance details in bulk.
   */
  @ApiOperation(value = "${swagger.operations.vehicle.bulk-compliance.description}",
      response = List.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Err/ No message available"),
      @ApiResponse(code = 400, message = "vrns missing or empty"),
      @ApiResponse(code = 200, message = "Compliance details for multiple vehicles")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "X-Correlation-ID", required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @PostMapping("/bulk-compliance")
  ResponseEntity<List<ComplianceResultsDto>> bulkCompliance(
      @RequestParam(value = "zones", required = false) String zones,
      @RequestBody List<String> vrns);


  /**
   * Get DVLA data for a vehicle.
   *
   * @param vrn Vehicle to fetch from DVLA.
   * @return DVLA data
   */
  @ApiOperation(value = "${swagger.operations.vehicle.dvla.description}",
      response = VehicleFromDvlaDto.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Correlation Id missing or invalid parameter"),
      @ApiResponse(code = 200, message = "DVLA data")
  })
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/{vrn}/external-details")
  ResponseEntity<VehicleFromDvlaDto> dvlaData(@PathVariable String vrn);

  /**
   * Get register details for vehicle.
   *
   * @param vrn string
   * @return Registered details about car
   */
  @ApiOperation(value = "${swagger.operations.register.details.description}",
      response = VehicleDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle registered details"),})
  @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true,
      value = "CorrelationID to track the request from the API gateway through"
          + " the Enquiries stack",
      paramType = "header")})
  @GetMapping("/{vrn}/register-details")
  ResponseEntity<RegisterDetailsDto> registerDetails(@PathVariable String vrn);

}