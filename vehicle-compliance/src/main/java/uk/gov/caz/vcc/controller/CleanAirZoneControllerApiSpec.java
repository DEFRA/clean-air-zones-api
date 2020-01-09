package uk.gov.caz.vcc.controller;

import static uk.gov.caz.vcc.controller.CleanAirZoneController.CLEAN_AIR_ZONES_PATH;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.vcc.dto.CleanAirZonesDto;

@RequestMapping(value = CLEAN_AIR_ZONES_PATH,
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Api(value = CLEAN_AIR_ZONES_PATH)
public interface CleanAirZoneControllerApiSpec {

  /**
   * Endpoint for retrieving a summary list of clean air zones and their boundary URLs.
   * @return a summary listing of a clean air zone including their identifiers and boundary urls.
   */
  @ApiOperation(
      value = "${swagger.operations.cleanAirZones.description}",
      response = CleanAirZonesDto.class
  )
  @ApiResponses(
      {@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Clean air zone listing details"),}
  )
  @ApiImplicitParams({
      @ApiImplicitParam(name = "X-Correlation-ID",
        required = true,
        value = "CorrelationID to track the request from the API gateway through"
            + " the Enquiries stack",
        paramType = "header")
  })
  @GetMapping("/clean-air-zones")
  ResponseEntity<CleanAirZonesDto> getCleanAirZones();
}
