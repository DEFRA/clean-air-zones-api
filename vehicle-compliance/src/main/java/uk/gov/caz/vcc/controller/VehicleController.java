package uk.gov.caz.vcc.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.definitions.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.definitions.exceptions.UnrecognisedFuelTypeException;
import uk.gov.caz.vcc.domain.exceptions.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.dto.RegisterDetailsDto;
import uk.gov.caz.vcc.dto.VehicleFromDvlaDto;
import uk.gov.caz.vcc.dto.validation.InvalidCazZoneFormat;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.ChargeCalculationService;
import uk.gov.caz.vcc.service.RegisterDetailsService;
import uk.gov.caz.vcc.service.UnrecognizedVehicleChargeCalculationService;
import uk.gov.caz.vcc.service.VehicleService;

/**
 * Rest Controller with endpoints related to vehicles.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleController implements VehicleControllerApiSpec {

  public static final String VEHICLES_PATH = "/v1/compliance-checker/vehicles";

  private final ChargeCalculationService chargeCalculationService;
  private final VehicleService vehicleService;
  private final UnrecognizedVehicleChargeCalculationService
      unrecognizedVehicleChargeCalculationService;
  private final CazTariffService cazTariffService;
  private final RegisterDetailsService registerDetailsService;

  @Value("${services.connection-timeout-seconds}")
  private Integer connectionTimeout;

  @Value("${services.read-timeout-seconds}")
  private Integer readTimeout;

  @Override
  public ResponseEntity<VehicleDto> details(@PathVariable String vrn) {
    return vehicleService.findVehicle(vrn)
        .map(vehicle -> ResponseEntity.status(HttpStatus.OK).body(vehicle))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @Override
  public ResponseEntity<ComplianceResultsDto> compliance(
      @PathVariable String vrn,
      @RequestParam(value = "zones", required = false) String zones) {
    try {
      ComplianceResultsDto result = chargeCalculationService.checkVrnAgainstCaz(vrn,
          prepareListOf(Optional.ofNullable(zones)));
      return ResponseEntity.ok().body(result);
    } catch (UnableToIdentifyVehicleComplianceException | UnrecognisedFuelTypeException ex) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          String.format("Unable to determine compliance due to %s", ex.getMessage()));
    }
  }

  @Override
  public ResponseEntity<VehicleTypeCazChargesDto> unrecognisedVehicle(
      @PathVariable("type") String vehicleType, @RequestParam("zones") String zones) {
    VehicleTypeCazChargesDto vehicleTypeCazChargesDto =
        unrecognizedVehicleChargeCalculationService.getCharges(vehicleType, parseZones(zones));

    return ResponseEntity.ok(vehicleTypeCazChargesDto);
  }

  @Override
  public ResponseEntity<List<ComplianceResultsDto>> bulkCompliance(String zones,
      List<String> vrns) {
    try {
      List<ComplianceResultsDto> complianceOutcomes = chargeCalculationService
          .bulkComplianceCheck(vrns, parseZones(zones));
      return ResponseEntity.ok().body(complianceOutcomes);
    } catch (InterruptedException e) {
      log.error("Bulk compliance exception {}", e.getMessage());
      Thread.currentThread().interrupt();
      return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }
  }

  @Override
  public ResponseEntity<RegisterDetailsDto> registerDetails(String vrn) {
    return ResponseEntity.ok(registerDetailsService.prepareRegisterDetails(vrn));
  }

  /**
   * Prepares {@link List} of {@link UUID} that are identifiers of Clean Air Zones.
   */
  private List<UUID> prepareListOf(Optional<String> zones) {
    return zones.map(VehicleController::parseZones).orElseGet(this::listOfAllZones);
  }

  /*
   * This method parses zones separated by comma.
   */
  static List<UUID> parseZones(String zones) {
    // See
    // https://stackoverflow.com/questions/43401777/passing-array-query-parameters-with-api-gateway-to-lambda?rq=1
    List<UUID> castZones = new ArrayList<>();
    
    if (zones == null) {
      return castZones;
    }
    
    String[] stringZones = zones.split(",");

    for (String stringZone : stringZones) {
      try {
        castZones.add(UUID.fromString(stringZone));
      } catch (IllegalArgumentException exceptions) {
        throw new InvalidCazZoneFormat(stringZone);
      }
    }

    return castZones;
  }

  /**
   * Returns list of UUIDs of all CAZes in the system.
   */
  private List<UUID> listOfAllZones() {
    return cazTariffService.getCleanAirZoneSelectionListings().getCleanAirZones().stream()
        .map(CleanAirZoneDto::getCleanAirZoneId).collect(
            Collectors.toList());
  }

  @Override
  public ResponseEntity<VehicleFromDvlaDto> dvlaData(String vrn) {
    try {
      Optional<Vehicle> vehicle = vehicleService.dvlaDataForVehicle(vrn);
      if (vehicle.isPresent()) {
        VehicleFromDvlaDto dto = VehicleFromDvlaDto.fromVehicle(vehicle.get());
        return ResponseEntity.ok(dto);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (NullPointerException exception) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}
