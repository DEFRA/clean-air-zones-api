package uk.gov.caz.taxiregister.dto.validation;

import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

abstract class ValidationErrorResolver {

  private final boolean lineNumberAware;
  private final int lineNumber;

  ValidationErrorResolver(VehicleDto vehicleDto) {
    this.lineNumberAware = vehicleDto.getLineNumber() > 0;
    this.lineNumber = lineNumberAware ? vehicleDto.getLineNumber() : -1;
  }

  final ValidationError missingFieldError(String vrm, String message) {
    return isLineNumberAware()
        ? ValidationError.missingFieldError(vrm, message, lineNumber)
        : ValidationError.missingFieldError(vrm, message);
  }

  final ValidationError valueError(String vrm, String message) {
    return isLineNumberAware()
        ? ValidationError.valueError(vrm, message, lineNumber)
        : ValidationError.valueError(vrm, message);
  }

  private boolean isLineNumberAware() {
    return lineNumberAware;
  }
}
