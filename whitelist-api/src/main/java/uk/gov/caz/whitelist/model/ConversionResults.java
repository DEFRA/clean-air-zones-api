package uk.gov.caz.whitelist.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Object which contains validation errors and two sets, one with vrn to delete and and second
 * whitelist objects to update or delete.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ConversionResults {

  List<ValidationError> validationErrors;

  Set<WhitelistVehicle> whitelistVehiclesToSaveOrUpdate;
  Set<String> vrnToDelete;

  public boolean hasValidationErrors() {
    return !validationErrors.isEmpty();
  }

  /**
   * Helper method to aggregate size of all vehicles.
   *
   * @return return total size of all vehicles.
   */
  public int size() {
    return whitelistVehiclesToSaveOrUpdate.size()
        + vrnToDelete.size();
  }

  /**
   * Creates an instance of {@link ConversionResults} from a list of {@link ConversionResult}. All
   * validation errors from each {@link ConversionResult} are flattened to one list. All
   * successfully converted vehicles are collected to a set in {@code retrofittedVehicles}
   * attribute.
   */
  public static ConversionResults from(List<ConversionResult> conversionResults) {

    Set<WhitelistVehicle> whitelistVehiclesToSaveOrUpdate = new HashSet<>();
    Set<String> vrnToDelete = new HashSet<>();
    List<ValidationError> validationErrors = new ArrayList<>();

    conversionResults.forEach(conversionResult -> {
      if (conversionResult.isFailure()) {
        validationErrors.addAll(conversionResult.getValidationErrors());
        return;
      }

      WhitelistVehicleCommand whitelistVehicleCommand = conversionResult
          .getWhitelistVehicleCommand();
      WhitelistVehicle whitelistVehicle = whitelistVehicleCommand.getWhitelistVehicle();
      if (Actions.DELETE.getActionCharacter().equals(whitelistVehicleCommand.getAction())) {
        vrnToDelete.add(whitelistVehicle.getVrn());
      } else {
        whitelistVehiclesToSaveOrUpdate.add(whitelistVehicle);
      }
    });

    return new ConversionResults(validationErrors, whitelistVehiclesToSaveOrUpdate,
        vrnToDelete);
  }
}
