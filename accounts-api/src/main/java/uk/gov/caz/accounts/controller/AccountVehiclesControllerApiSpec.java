package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
import static uk.gov.caz.accounts.controller.AccountVehiclesController.CSV_EXPORTS;
import static uk.gov.caz.accounts.controller.AccountVehiclesController.SINGLE_VEHICLE_PATH_SEGMENT;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.accounts.dto.AccountVehicleRequest;
import uk.gov.caz.accounts.dto.AccountVehicleResponse;
import uk.gov.caz.accounts.dto.CsvExportResponse;
import uk.gov.caz.definitions.dto.accounts.ChargeableVehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;

@RequestMapping(path = ACCOUNT_VEHICLES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface AccountVehiclesControllerApiSpec {

  /**
   * Endpoint specification that handles single AccountVehicle creation.
   *
   * @param accountId Account identifier to which a vehicle is going to be added.
   * @param accountVehicleRequest vehicle details.
   * @return {@link AccountVehicleResponse} representing created object.
   */
  @ApiOperation(value = "${swagger.operations.account-vehicles.create.description}",
      response = AccountVehicleResponse.class)
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Account Vehicle creation Failed"),
      @ApiResponse(code = 404, message = "Account with a given ID was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 201, message = "Account Vehicle Created")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path")})
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<AccountVehicleResponse> createVehicle(@PathVariable("accountId") String accountId,
      @RequestBody AccountVehicleRequest accountVehicleRequest);

  /**
   * Endpoint specification that returns a page of vehicles associated with an account using
   * offset.
   *
   * @param accountId string representing accountId.
   * @param map the query strings included in the request
   * @return {@link VehiclesResponseDto}
   */
  @ApiOperation(value =
      "${swagger.operations.accounts.account-vehicles.retrieve-offset.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 200, message = "Vehicles returned successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from AccountVehicle table"
              + " (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "pageNumber", required = true, value = "The starting page number",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize", required = true, value = "The size of the page",
          paramType = "query"),
      @ApiImplicitParam(name = "onlyChargeable",
          value = "Flag used to filter only chargeable vehicles", paramType = "query")})
  @GetMapping
  ResponseEntity<VehiclesResponseDto> getAccountVehicleVrnsWithOffset(
      @PathVariable("accountId") UUID accountId,
      @RequestParam Map<String, String> map);

  /**
   * Endpoint specification that returns a cursor-based page of vrns associated with an account.
   *
   * @param accountId string representing accountId.
   * @param map the query strings included in the request
   * @return a list of vrns matching accountId and corresponding to other params
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.account-vehicles.retrieve-cursor.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 200, message = "List of vrns returned successfully")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from AccountVehicle table"
              + " (must match PK in Account table)"),
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "direction",
          required = true,
          value = "The direction in which to sort",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize",
          required = true,
          value = "The size of the page",
          paramType = "query"),
      @ApiImplicitParam(name = "vrn",
          required = true,
          value = "The cursor from which to start the page",
          paramType = "query"),
      @ApiImplicitParam(name = "chargeableCazId",
          value = "UUID formatted string with CAZ identifier",
          required = true,
          paramType = "query")
  })
  @GetMapping(path = "/sorted-page")
  ResponseEntity<ChargeableVehiclesResponseDto> getAccountVehicleVrnsWithCursor(
      @PathVariable("accountId") String accountId,
      @RequestParam Map<String, String> map);

  /**
   * Endpoint specification that handles removal of AccountVehicle.
   *
   * @param accountId Account identifier from which the vehicle is going to be removed.
   * @param vrn identifies AccountVehicle to remove.
   */
  @ApiOperation(value = "${swagger.operations.account-vehicles.delete.description}")
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account with a given ID was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 204, message = "Account Vehicle Deleted")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "vrn", required = true,
          value = "Vehicle Registration Number specifying the AccountVehicle", paramType = "path")})
  @DeleteMapping(path = SINGLE_VEHICLE_PATH_SEGMENT)
  ResponseEntity<Void> deleteVehicle(@PathVariable("accountId") String accountId,
      @PathVariable("vrn") String vrn);

  /**
   * Endpoint specification that handles retrieval of a single vehicle associated with an account.
   *
   * @param accountId Account identifier from which the vehicle is going to be fetched.
   * @param vrn identifies AccountVehicle to retrieve.
   */
  @ApiOperation(value = "${swagger.operations.account-vehicles.get.description}")
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Vehicle for the given account ID was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Account Vehicle found")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "vrn", required = true,
          value = "Vehicle Registration Number specifying the AccountVehicle", paramType = "path")})
  @GetMapping(path = SINGLE_VEHICLE_PATH_SEGMENT)
  ResponseEntity<VehicleWithCharges> getVehicle(@PathVariable("accountId") UUID accountId,
      @PathVariable("vrn") String vrn);

  /**
   * Endpoint specification that handles exporting csv with vehicles for given accountId.
   *
   * @param accountId Account identifier from which the vehicle is going to be fetched.
   */
  @ApiOperation(value = "${swagger.operations.csv-export.description}")
  @ApiResponses({@ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Csv exported successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path")})
  @PostMapping(path = CSV_EXPORTS)
  ResponseEntity<CsvExportResponse> csvExport(@PathVariable("accountId") UUID accountId);
}
