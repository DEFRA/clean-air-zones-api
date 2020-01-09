package uk.gov.caz.vcc.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.vcc.controller.CacheInvalidationsController.CACHE_INVALIDATION_PATH;

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
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.vcc.dto.VehicleResultsDto;
import uk.gov.caz.vcc.dto.VrmsDto;

/**
 * Interface with swagger documentation for CacheInvalidationsController.
 */
@RequestMapping(value = CACHE_INVALIDATION_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = CACHE_INVALIDATION_PATH)
public interface CacheInvalidationsControllerApiSpec {

  /**
   * This method allows for evicting a cached clean-air-zones from redis.
   */
  @ApiOperation(value = "${swagger.operations.cacheInvalidation.cleanAirZones.description}",
      response = VehicleResultsDto.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 202, message = "Accepted Request"),})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @PostMapping("/clean-air-zones")
  ResponseEntity<Void> cacheEvictCleanAirZones();

  /**
   * This method allows for evicting a cached clean-air-zones from redis.
   */
  @ApiOperation(value = "${swagger.operations.cacheInvalidation.licences.description}",
      response = VehicleResultsDto.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 202, message = "Accepted Request"),})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @PostMapping("/licences")
  ResponseEntity<Void> cacheEvictLicences(@RequestBody VrmsDto vrms);
  
  /**
   * This method allows for evicting all cached vehicle details.
   */
  @ApiOperation(value = "${swagger.operations.cacheInvalidation.licences.description}",
      response = VehicleResultsDto.class)
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 202, message = "Accepted Request"),})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @PostMapping("/vehicles")
  ResponseEntity<Void> cacheEvictVehicles();
}