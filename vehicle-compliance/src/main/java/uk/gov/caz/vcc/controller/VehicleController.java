package uk.gov.caz.vcc.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import uk.gov.caz.vcc.domain.service.compliance.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.dto.VehicleDto;
import uk.gov.caz.vcc.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.vcc.dto.validation.InvalidCazZoneFormat;
import uk.gov.caz.vcc.service.ChargeCalculationService;
import uk.gov.caz.vcc.service.UnrecognizedVehicleChargeCalculationService;
import uk.gov.caz.vcc.service.VehicleService;

/**
 * Rest Controller with endpoints related to vehicles.
 */
@RestController
@AllArgsConstructor
public class VehicleController implements VehicleControllerApiSpec {

  public static final String VEHICLES_PATH = "/v1/compliance-checker/vehicles";

  private final VehicleService vehicleService;

  private final ChargeCalculationService chargeCalculationService;

  private final UnrecognizedVehicleChargeCalculationService
      unrecognizedVehicleChargeCalculationService;

  @Override
  public ResponseEntity<VehicleDto> details(@PathVariable String vrn) {
    return vehicleService.findVehicle(vrn)
        .map(vehicle -> ResponseEntity.status(HttpStatus.OK).body(vehicle))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @Override
  public ResponseEntity<ComplianceResultsDto> compliance(
      @PathVariable String vrn, @RequestParam("zones") String zones) {

    try {
      ComplianceResultsDto result =
          chargeCalculationService.checkVrnAgainstCaz(vrn, parseZones(zones));
      return ResponseEntity.ok().body(result);
    } catch (UnableToIdentifyVehicleComplianceException ex) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, String.format("Unable to determine compliance due to %s", ex.getMessage()));
    }
  }

  @Override
  public ResponseEntity<VehicleTypeCazChargesDto> unrecognisedVehicle(
      @PathVariable("type") String vehicleType, @RequestParam("zones") String zones) {
    VehicleTypeCazChargesDto vehicleTypeCazChargesDto =
        unrecognizedVehicleChargeCalculationService.getCharges(vehicleType, parseZones(zones));

    return ResponseEntity.ok(vehicleTypeCazChargesDto);
  }

  /*
  This method parses zones separated by comma.
   */
  static List<UUID> parseZones(String zones) {
    // See
    // https://stackoverflow.com/questions/43401777/passing-array-query-parameters-with-api-gateway-to-lambda?rq=1
    List<UUID> castZones = new ArrayList<>();
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
}
