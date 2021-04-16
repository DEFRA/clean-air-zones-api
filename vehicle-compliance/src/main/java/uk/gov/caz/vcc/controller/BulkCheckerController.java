package uk.gov.caz.vcc.controller;

import java.io.FileNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.caz.vcc.service.BulkCheckerService;
import uk.gov.caz.vcc.service.BulkCheckerService.BulkCheckerCsvFile;

/**
 * Rest Controller with endpoints related to bulk checker.
 */
@RestController
@RequiredArgsConstructor
public class BulkCheckerController implements BulkCheckerControllerApiSpec {
  public static final String PATH = "/v1/bulk-compliance-checker/progress";
  private final BulkCheckerService bulkCheckerService;

  @Override
  public ResponseEntity<BulkCheckerCsvFile>
      checkProgress(@PathVariable("filename") String fileName) {
    try {
      BulkCheckerCsvFile bulkCheckerCsvFile = bulkCheckerService.getBulkCheckerOutputFile(fileName);
      if (bulkCheckerCsvFile.getFileName() != null) {
        return ResponseEntity.status(HttpStatus.OK).body(bulkCheckerCsvFile);
      } else {
        return ResponseEntity.status(HttpStatus.OK).body(null);
      }
    } catch (FileNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
  }
}