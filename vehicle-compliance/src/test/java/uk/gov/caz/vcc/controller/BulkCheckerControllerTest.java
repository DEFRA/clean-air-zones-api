package uk.gov.caz.vcc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.caz.vcc.service.BulkCheckerService;
import uk.gov.caz.vcc.service.BulkCheckerService.BulkCheckerCsvFile;

@ExtendWith(MockitoExtension.class)
public class BulkCheckerControllerTest {
  @Mock
  BulkCheckerService bulkCheckerService;

  @Test
  public void shouldReturnOKWithFilePath() {
    try {
      BulkCheckerCsvFile bulkCheckerCsvFile = new BulkCheckerCsvFile("s3Bucket", "fileName");
      when(bulkCheckerService.getBulkCheckerOutputFile(anyString())).thenReturn(bulkCheckerCsvFile);
      BulkCheckerController controller = new BulkCheckerController(bulkCheckerService);
      ResponseEntity<BulkCheckerCsvFile> responseEntity = controller.checkProgress("fileName");
      assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      assertNotNull(responseEntity.getBody().getFileName());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void shouldReturnOKWithNullBody() {
    try {
      BulkCheckerCsvFile bulkCheckerCsvFile = new BulkCheckerCsvFile("s3Bucket", null);
      when(bulkCheckerService.getBulkCheckerOutputFile(anyString())).thenReturn(bulkCheckerCsvFile);
      BulkCheckerController controller = new BulkCheckerController(bulkCheckerService);
      ResponseEntity<BulkCheckerCsvFile> responseEntity = controller.checkProgress("fileName");
      assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      assertNull(responseEntity.getBody());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void shouldReturn404() {
    try {
      when(bulkCheckerService.getBulkCheckerOutputFile(anyString())).thenThrow(FileNotFoundException.class);
      BulkCheckerController controller = new BulkCheckerController(bulkCheckerService);
      ResponseEntity<BulkCheckerCsvFile> responseEntity = controller.checkProgress("fileName");
      assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
      assertNull(responseEntity.getBody());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}