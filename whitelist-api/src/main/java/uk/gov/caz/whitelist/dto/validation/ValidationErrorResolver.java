package uk.gov.caz.whitelist.dto.validation;

import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.service.validation.CsvAwareValidationMessageModifier;

abstract class ValidationErrorResolver {
  private static final CsvAwareValidationMessageModifier messageModifier =
      new CsvAwareValidationMessageModifier();

  private final boolean lineNumberAware;
  private final int lineNumber;

  ValidationErrorResolver(WhitelistedVehicleDto whitelistedVehicleDto) {
    this.lineNumberAware = whitelistedVehicleDto.getLineNumber() > 0;
    this.lineNumber = lineNumberAware ? whitelistedVehicleDto.getLineNumber() : -1;
  }

  private String addCsvHeaderPresentInfoIfApplicable(String message) {
    return isLineNumberAware()
        ? messageModifier.addHeaderRowInfoSuffix(message, lineNumber)
        : message;
  }

  final ValidationError missingFieldError(String vrn, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.missingFieldError(vrn, embellishedMessage, lineNumber)
        : ValidationError.missingFieldError(vrn, embellishedMessage);
  }

  final ValidationError valueError(String vrn, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.valueError(vrn, embellishedMessage, lineNumber)
        : ValidationError.valueError(vrn, embellishedMessage);
  }

  private boolean isLineNumberAware() {
    return lineNumberAware;
  }
}
