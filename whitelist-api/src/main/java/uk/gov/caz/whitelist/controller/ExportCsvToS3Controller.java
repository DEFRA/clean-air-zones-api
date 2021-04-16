package uk.gov.caz.whitelist.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.whitelist.dto.ExportCsvResponseDto;
import uk.gov.caz.whitelist.service.WhitelistVehiclesExporter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExportCsvToS3Controller implements ExportCsvToS3ControllerApiSpec {

  public static final String BASE_PATH = "/v1/whitelisting/vehicles/csv-exports";

  private final WhitelistVehiclesExporter whitelistVehiclesExporter;

  @Override
  public ResponseEntity<ExportCsvResponseDto> exportCsvToS3() {
    return new ResponseEntity<>(exportAndCreateResponse(), HttpStatus.CREATED);
  }

  private ExportCsvResponseDto exportAndCreateResponse() {
    String destinationUrl = whitelistVehiclesExporter.export();
    return ExportCsvResponseDto.builder()
        .bucketName(whitelistVehiclesExporter.getS3DestinationBucket())
        .fileUrl(destinationUrl)
        .build();
  }
}