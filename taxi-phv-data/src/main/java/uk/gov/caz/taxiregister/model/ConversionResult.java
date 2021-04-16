package uk.gov.caz.taxiregister.model;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true, access = AccessLevel.PRIVATE)
public class ConversionResult {
  @Singular
  List<ValidationError> validationErrors;

  TaxiPhvVehicleLicence licence;

  public boolean isSuccess() {
    return licence != null && validationErrors.isEmpty();
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public static ConversionResult success(TaxiPhvVehicleLicence licence) {
    return new ConversionResult(Collections.emptyList(), licence);
  }

  public static ConversionResult failure(List<ValidationError> validationErrors) {
    return new ConversionResult(validationErrors, null);
  }

  /**
   * Method that combines non unique vehicle error with errors that are already stored in the List.
   */
  public ConversionResult withError(ValidationError additionalError) {
    return toBuilder()
        .validationError(additionalError)
        .build();
  }
}
