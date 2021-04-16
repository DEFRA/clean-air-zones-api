package uk.gov.caz.tariff.controller;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.tariff.dto.Tariff;
import uk.gov.caz.tariff.service.CleanAirZonesRepository;
import uk.gov.caz.tariff.service.TariffRepository;

/**
 * REST Controller exposing endpoints related to Clean Air Zones.
 */
@RestController
@Slf4j
public class CleanAirZonesController implements CleanAirZonesControllerApiSpec {

  public static final String PATH = "/v1/clean-air-zones";

  private final TariffRepository tariffRepository;

  private final CleanAirZonesRepository cleanAirZonesRepository;

  public CleanAirZonesController(TariffRepository tariffRepository,
      CleanAirZonesRepository cleanAirZonesRepository) {
    this.tariffRepository = tariffRepository;
    this.cleanAirZonesRepository = cleanAirZonesRepository;
  }

  @Override
  public ResponseEntity<CleanAirZonesDto> cleanAirZones() {
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(cleanAirZonesRepository.findAll());
  }

  @Override
  public ResponseEntity<Tariff> tariff(@PathVariable String cleanAirZoneId) {
    return tariffRepository.findByCleanAirZoneId(UUID.fromString(cleanAirZoneId))
        .map(tariff -> ResponseEntity
            .status(HttpStatus.OK)
            .body(tariff))
        .orElseGet(() -> ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .build());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity handleIllegalArgumentException(Exception e) {
    log.error("Unhandled exception: ", e);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}