package uk.gov.caz.whitelist.model;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionResult {
  List<ValidationError> validationErrors;

  WhitelistVehicleCommand whitelistVehicleCommand;

  public boolean isSuccess() {
    return whitelistVehicleCommand != null;
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public static ConversionResult success(WhitelistVehicleCommand whitelistVehicleCommand) {
    return new ConversionResult(Collections.emptyList(), whitelistVehicleCommand);
  }

  public static ConversionResult failure(List<ValidationError> validationErrors) {
    return new ConversionResult(validationErrors, null);
  }
}
