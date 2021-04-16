package uk.gov.caz.vcc.controller;

import static uk.gov.caz.vcc.controller.BulkCheckerController.PATH;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import uk.gov.caz.vcc.service.BulkCheckerService.BulkCheckerCsvFile;

/**
 * Interface with swagger documentation for BulkCheckerController.
 */
@RequestMapping(value = PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE
})
public interface BulkCheckerControllerApiSpec {
  /**
   * Query bulk checker processing status.
   * @param fileName Csv filename.
   * @return Path to Csv output file located in S3.
   */
  @ApiOperation(value = "${swagger.operations.bulkChecker.statusCheck.description}",
      response = BulkCheckerCsvFile.class)
  @ApiResponses({
      @ApiResponse(code = 404, message = "File not found"),
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 200, message = "Query succeeded"),
  })
  @GetMapping("/{filename}")
  ResponseEntity<BulkCheckerCsvFile> checkProgress(@PathVariable("filename") String fileName);
}