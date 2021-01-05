package uk.gov.caz.accounts.dto.validation;

import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.registerjob.ValidationError;
import uk.gov.caz.accounts.service.validation.CsvAwareValidationMessageModifier;

abstract class ValidationErrorResolver {
  private static final CsvAwareValidationMessageModifier messageModifier =
      new CsvAwareValidationMessageModifier();

  private final boolean lineNumberAware;
  private final int lineNumber;

  ValidationErrorResolver(AccountVehicleDto accountVehicleDto) {
    this.lineNumberAware = accountVehicleDto.getLineNumber() > 0;
    this.lineNumber = lineNumberAware ? accountVehicleDto.getLineNumber() : -1;
  }

  private String addCsvHeaderPresentInfoIfApplicable(String message) {
    return isLineNumberAware()
        ? messageModifier.addHeaderRowInfoSuffix(message, lineNumber)
        : message;
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
