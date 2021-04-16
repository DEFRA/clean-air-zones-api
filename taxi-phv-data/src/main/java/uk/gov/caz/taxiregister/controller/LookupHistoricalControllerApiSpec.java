package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResponse;

/**
 * Rest Controller which provides licence historical info.
 */
@RequestMapping(
    produces = {MediaType.APPLICATION_JSON_VALUE},
    consumes = {MediaType.APPLICATION_JSON_VALUE}
)
@Api(value = LookupHistoricalController.PATH)
public interface LookupHistoricalControllerApiSpec {

  /**
   * Looks up vehicle's information about its historical data for given VRM inside provided date
   * range.
   *
   * @return An instance of {@link LicenceInfoHistoricalResponse}.
   */
  @ApiOperation(
      value = "${swagger.operations.lookup-history.description}",
      response = LicenceInfoHistoricalResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 200,
          message = "Historical data for given VRM inside provided date range"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 500, message = "Internal error"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "vrm",
          required = true,
          value = "The vrm to query",
          paramType = "path"),
      @ApiImplicitParam(name = "startDate",
          required = true,
          value = "Date that describes modification date from",
          paramType = "query"),
      @ApiImplicitParam(name = "endDate",
          required = true,
          value = "Date that describes modification date until",
          paramType = "query"),
      @ApiImplicitParam(name = "pageNumber",
          required = true,
          value = "The number of the page to be retrieved",
          paramType = "query"),
      @ApiImplicitParam(name = "pageSize",
          required = true,
          value = "The size of the page to be retrieved",
          paramType = "query"),
  })
  @GetMapping(LookupHistoricalController.PATH)
  ResponseEntity<LicenceInfoHistoricalResponse> getLicenceInfoFor(@PathVariable String vrm,
      @RequestParam Map<String, String> queryStrings);
}