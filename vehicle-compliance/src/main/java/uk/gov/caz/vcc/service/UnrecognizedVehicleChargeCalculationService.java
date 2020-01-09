package uk.gov.caz.vcc.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;
import uk.gov.caz.vcc.dto.ChargeDto;
import uk.gov.caz.vcc.dto.VehicleTypeCazChargesDto;

/**
 * Service class that calculates charge for non-uk vehicle.
 */
@Service
@Slf4j
@AllArgsConstructor
public class UnrecognizedVehicleChargeCalculationService {

  private final CazTariffService cazTariffService;

  /**
   * Constructs response by fetching all tariffs from Tariff service and matching with given vehicle
   * type
   *
   * @param vehicleType - vehicle type: bus, mini_van etc.
   * @param cazIds - IDs of Clear Air Zones
   * @return VehicleTypeCazChargesDto
   */
  public VehicleTypeCazChargesDto getCharges(String vehicleType, List<UUID> cazIds) {
    log.info("Processing [{}] CAZ IDs for type [{}]", cazIds.size(), vehicleType);

    List<TariffDetails> tariffDetails = getTariffs(cazIds);
    List<TariffDetails> tariffsForGivenVehicleType = getTariffsForGivenVehicleType(vehicleType,
        tariffDetails);

    return mapToResponse(vehicleType, tariffsForGivenVehicleType);
  }

  /**
   * Creates response that wraps all matching charges for given vehicle type.
   *
   * @param type - vehicle type
   * @param tariffsForGivenVehicleType all tariffs that matches vehicle type
   * @return VehicleTypeCazChargesDto - DTO object serialized to the response
   */
  private VehicleTypeCazChargesDto mapToResponse(String type,
      List<TariffDetails> tariffsForGivenVehicleType) {
    List<ChargeDto> charges = tariffsForGivenVehicleType.stream()
        .map(e -> {
          VehicleTypeCharge vehicleTypeCharge = chargeForGivenVehicleType(type, e).orElseThrow(
              () -> new RuntimeException("Charge for given vehicle should exist at this step"));
          return new ChargeDto(
              e.getCazId(),
              e.getName(),
              vehicleTypeCharge.getCharge()
          );
        })
        .collect(Collectors.toList());

    return new VehicleTypeCazChargesDto(charges);
  }

  /**
   * Filters tariffs by vehicle type.
   */
  @NotNull
  private List<TariffDetails> getTariffsForGivenVehicleType(String type,
      List<TariffDetails> tariffDetails) {
    return tariffDetails
        .stream()
        .filter(tariff -> tariffExistsForGivenType(type, tariff))
        .collect(Collectors.toList());
  }

  /**
   * Fetches all tariffs for given caz ids.
   */
  @NotNull
  private List<TariffDetails> getTariffs(List<UUID> cazIds) {
    List<TariffDetails> tariffs = cazIds.stream()
        .map(cazTariffService::getTariffDetailsForGivenCazId)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
    log.info("Fetched [{}] tariff details from Tariff service", tariffs.size());

    return tariffs;
  }

  /**
   * Returns charge for given vehicle, if exists.
   */
  @VisibleForTesting
  Optional<VehicleTypeCharge> chargeForGivenVehicleType(String type,
      TariffDetails tariffDetails) {
    return tariffDetails.getRates()
        .stream()
        .filter(vehicleTypesMatches(type))
        .findFirst();
  }

  /**
   * Checks whether charge exists for given vehicle tupe.
   */
  @VisibleForTesting
  boolean tariffExistsForGivenType(String type, TariffDetails tariffDetails) {
    return tariffDetails.getRates()
        .stream()
        .anyMatch(vehicleTypesMatches(type));
  }

  /**
   * Case insensitive check between charge's vehicle type and vehicle type from request.
   */
  @NotNull
  @VisibleForTesting
  Predicate<VehicleTypeCharge> vehicleTypesMatches(String vehicleType) {
    return charge -> charge.getVehicleType().name().toLowerCase()
        .equalsIgnoreCase(vehicleType.toLowerCase());
  }
}
