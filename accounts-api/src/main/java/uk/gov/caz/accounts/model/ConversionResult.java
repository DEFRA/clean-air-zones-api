package uk.gov.caz.accounts.model;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.util.CollectionUtils;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

@Value
@Builder(toBuilder = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionResult {

  @Singular
  List<ValidationError> validationErrors;
  AccountVehicle accountVehicle;

  public boolean isSuccess() {
    return accountVehicle != null && CollectionUtils.isEmpty(validationErrors);
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public static ConversionResult success(AccountVehicle accountVehicle) {
    return new ConversionResult(Collections.emptyList(), accountVehicle);
  }

  public static ConversionResult failure(List<ValidationError> validationErrors) {
    return new ConversionResult(validationErrors, null);
  }

  /**
   * Combines non unique vehicle error with errors that are already stored in the List.
   */
  public ConversionResult withError(ValidationError additionalError) {
    return toBuilder()
        .validationError(additionalError)
        .build();
  }
}
