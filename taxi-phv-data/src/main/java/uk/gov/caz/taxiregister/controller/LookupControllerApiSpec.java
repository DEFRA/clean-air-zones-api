package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.taxiregister.dto.LicenceInfo;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoRequestDto;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoResponseDto;

@RequestMapping(
    produces = {MediaType.APPLICATION_JSON_VALUE},
    consumes = {MediaType.APPLICATION_JSON_VALUE}
)
@Api(value = LookupController.PATH)
public interface LookupControllerApiSpec {

  /**
   * Looks up information about licences for a given vehicle identified by VRM.
   *
   * @param vrm VRM of a vehicle the licence information will be looked for
   * @return An instance of {@link LicenceInfo} that contains two flags: whether a vehicle has any
   *     active operating licence and is wheelchair accessible for any active licence. The latter
   *     can be {@code null} if this is not determined.
   */
  @ApiOperation(
      value = "${swagger.operations.lookup.description}",
      response = LicenceInfo.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 404, message = "Not Found / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @GetMapping(LookupController.PATH)
  ResponseEntity<LicenceInfo> getLicenceInfoFor(@PathVariable String vrm);

  /**
   * Looks up information about licences for given vehicles identified by VRMs.
   *
   * @param request Object containing a list of VRMs of vehicles the licence information will be
   *     looked for
   * @return An instance of {@link GetLicencesInfoResponseDto}.
   */
  @ApiOperation(
      value = "${swagger.operations.batch-lookup.description}",
      response = GetLicencesInfoResponseDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping(LookupController.BULK_PATH)
  ResponseEntity<GetLicencesInfoResponseDto> getLicencesInfoFor(
      @RequestBody GetLicencesInfoRequestDto request);
}