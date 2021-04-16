package uk.gov.caz.whitelist.service;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.whitelist.controller.exception.VrnAlreadyWhitelistedException;
import uk.gov.caz.whitelist.model.Actions;
import uk.gov.caz.whitelist.model.ConversionResult;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.model.WhitelistVehicleCommand;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;
import uk.gov.caz.whitelist.service.exception.VehicleNotFoundException;

@Service
@AllArgsConstructor
public class WhitelistService {

  private final WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;
  private final RegisterService registerService;

  /**
   * Finds single entity of {@link WhitelistVehicle}. If none is found, {@link Optional#empty()} is
   * returned.
   *
   * @param vrn which will identify {@link WhitelistVehicle}.
   * @return single found {@link WhitelistVehicle}.
   */
  public Optional<WhitelistVehicle> findBy(String vrn) {
    return whitelistVehiclePostgresRepository.findOneByVrn(vrn);
  }

  /**
   * Method to create and return whitelist vehicle.
   *
   * @param whitelistVehicle {@link WhitelistVehicle}
   * @return {@link WhitelistVehicle}
   */
  @Transactional
  public WhitelistVehicle save(WhitelistVehicle whitelistVehicle) {
    boolean vehicleExists = whitelistVehiclePostgresRepository.exists(whitelistVehicle.getVrn());
    if (vehicleExists) {
      throw new VrnAlreadyWhitelistedException("VRN is already whitelisted.");
    }

    registerService
        .register(mapToConversionResultsWithCommand(whitelistVehicle, Actions.CREATE),
            whitelistVehicle.getUploaderId(),
            whitelistVehicle.getUploaderEmail());

    return findBy(whitelistVehicle.getVrn()).orElseThrow(NoSuchElementException::new);
  }

  /**
   * Deletes a vehicle from the database identified by the passed {@code vrn}.
   *
   * @param vrn Vehicle registration number, a mean to identify the vehicle.
   * @param modifierId UUID of user that is making a modification.
   * @throws VehicleNotFoundException if the vehicle is absent in the database.
   */
  @Transactional
  public WhitelistVehicle deleteBy(String vrn, UUID modifierId, String modifierEmail) {
    WhitelistVehicle vehicleToBeDeleted = whitelistVehiclePostgresRepository
        .findOneByVrn(vrn)
        .orElseThrow(VehicleNotFoundException::new);

    registerService
        .register(mapToConversionResultsWithCommand(vehicleToBeDeleted, Actions.DELETE),
            modifierId, modifierEmail);

    return vehicleToBeDeleted;
  }

  @Transactional
  public void delete(List<WhitelistVehicle> vehicles, UUID modifierId,
      String modifierEmail) {
    registerService.register(mapToConversionResultsWithCommand(vehicles, Actions.DELETE),
            modifierId, modifierEmail);
  }

  /**
   * Maps {@link WhitelistVehicle} to command/action specified by action parameter so it can be
   * ultimately mapped to {@link ConversionResults} object required by {@link RegisterService}.
   */
  private ConversionResults mapToConversionResultsWithCommand(WhitelistVehicle whitelistVehicle,
      Actions action) {
    return ConversionResults
        .from(singletonList(asConversionResult(whitelistVehicle, action)));
  }

  private ConversionResults mapToConversionResultsWithCommand(List<WhitelistVehicle> vehicles,
      Actions action) {
    return ConversionResults.from(asConversionResults(vehicles, action));
  }

  /**
   * Maps {@link WhitelistVehicle} to command/action specified by action parameter so it can be
   * ultimately mapped to {@link ConversionResults} object required by {@link RegisterService}.
   */
  private ConversionResult asConversionResult(WhitelistVehicle whitelistVehicle, Actions action) {
    return ConversionResult.success(toCommand(whitelistVehicle, action));
  }

  private List<ConversionResult> asConversionResults(List<WhitelistVehicle> whitelistVehicle,
      Actions action) {
    return whitelistVehicle.stream()
        .map(vehicle -> ConversionResult.success(toCommand(vehicle, action)))
        .collect(Collectors.toList());
  }

  /**
   * Maps {@link WhitelistVehicle} to command/action specified by action parameter so it can be
   * ultimately mapped to {@link ConversionResults} object required by {@link RegisterService}.
   */
  private WhitelistVehicleCommand toCommand(WhitelistVehicle whitelistVehicle, Actions action) {
    return WhitelistVehicleCommand.builder()
        .whitelistVehicle(whitelistVehicle)
        .action(action.getActionCharacter())
        .build();
  }
}