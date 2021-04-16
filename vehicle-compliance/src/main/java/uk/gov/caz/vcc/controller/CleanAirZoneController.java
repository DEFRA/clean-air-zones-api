package uk.gov.caz.vcc.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.service.CazTariffService;

/**
 * Rest Controller with endpoints related to clear air zone.
 */
@RestController
@AllArgsConstructor
public class CleanAirZoneController implements CleanAirZoneControllerApiSpec {

  public static final String CLEAN_AIR_ZONES_PATH = "/v1/compliance-checker";

  private final CazTariffService cazTariffService;

  @Override
  public ResponseEntity<CleanAirZonesDto> getCleanAirZones() {

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(cazTariffService.getCleanAirZoneSelectionListings());
  }
}
