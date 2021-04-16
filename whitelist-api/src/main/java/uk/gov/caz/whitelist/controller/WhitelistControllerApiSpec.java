package uk.gov.caz.whitelist.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.whitelist.controller.WhitelistController.X_MODIFIER_EMAIL_HEADER;
import static uk.gov.caz.whitelist.controller.WhitelistController.X_MODIFIER_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDetailsResponseDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleRequestDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleResponseDto;

@RequestMapping(
    value = WhitelistController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface WhitelistControllerApiSpec {

  /**
   * Returns whitelisted vehicle details.
   *
   * @return {@link WhitelistedVehicleResponseDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.whitelisted-vehicle-details.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @GetMapping("/{vrn}")
  ResponseEntity<WhitelistedVehicleDetailsResponseDto> whitelistVehicleDetails(
      @PathVariable String vrn);

  /**
   * Create and return whitelisted vehicle details.
   *
   * @param dto {@link WhitelistedVehicleRequestDto}
   * @return {@link WhitelistedVehicleResponseDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(value = "${swagger.operations.whitelisted-vehicle-details.description}",
      response = WhitelistedVehicleResponseDto.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 200, message = "Vehicle Results"),})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization", required = true,
          value = "OAuth 2.0 authorisation token.", paramType = "header"),
      @ApiImplicitParam(name = "timestamp", required = true,
          value = "ISO 8601 formatted datetime string indicating when the request was made.",
          paramType = "header")})
  @PostMapping
  ResponseEntity<WhitelistedVehicleResponseDto> addWhitelistVehicle(
      @RequestBody WhitelistedVehicleRequestDto dto);

  /**
   * Deletes whitelisted vehicle for given VRN.
   *
   * @return {@link WhitelistedVehicleResponseDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.whitelisted-vehicle-delete.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Vehicle removed successfully")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = X_MODIFIER_ID_HEADER,
          required = true,
          value = "UUID with identifier/sub of user making a modification",
          paramType = "header"),
      @ApiImplicitParam(name = X_MODIFIER_EMAIL_HEADER,
          required = true,
          value = "Email of user making a modification",
          paramType = "header")
  })
  @DeleteMapping("/{vrn}")
  ResponseEntity<WhitelistedVehicleResponseDto> removeWhitelistVehicle(
      @PathVariable String vrn, @RequestHeader(X_MODIFIER_ID_HEADER) UUID modifierId,
      @RequestHeader(X_MODIFIER_EMAIL_HEADER) String modifierEmail);
}