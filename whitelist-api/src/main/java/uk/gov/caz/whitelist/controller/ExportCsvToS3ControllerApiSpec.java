package uk.gov.caz.whitelist.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.whitelist.dto.ExportCsvResponseDto;

@RequestMapping(
    value = ExportCsvToS3Controller.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface ExportCsvToS3ControllerApiSpec {

  /**
   * Returns the details of an exported csv file.
   *
   * @return {@link ExportCsvResponseDto} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.export-csv.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 201, message = "File exported to S3"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PostMapping
  ResponseEntity<ExportCsvResponseDto> exportCsvToS3();
}