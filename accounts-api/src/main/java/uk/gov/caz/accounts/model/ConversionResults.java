package uk.gov.caz.accounts.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ConversionResults {

  List<ValidationError> validationErrors;

  Set<AccountVehicle> accountVehicles;

  public boolean hasValidationErrors() {
    return !validationErrors.isEmpty();
  }

  /**
   * Helper method to return size of stored AccountVehicles.
   *
   * @return return total size of all vehicles.
   */
  public int size() {
    return accountVehicles.size();
  }

  /**
   * Creates an instance of {@link ConversionResults} from a list of {@link ConversionResult}. All
   * validation errors from each {@link ConversionResult} are flattened to one list. All
   * successfully converted vehicles are collected to a set in {@code accountVehicles} attribute.
   */
  public static ConversionResults from(List<ConversionResult> conversionResults) {
    Set<AccountVehicle> accountVehicles = new HashSet<>();
    List<ValidationError> validationErrors = new ArrayList<>();

    conversionResults.forEach(conversionResult -> {
      if (conversionResult.isFailure()) {
        validationErrors.addAll(conversionResult.getValidationErrors());
        return;
      }

      AccountVehicle accountVehicle = conversionResult.getAccountVehicle();
      accountVehicles.add(accountVehicle);
    });

    return new ConversionResults(validationErrors, accountVehicles);
  }
}
