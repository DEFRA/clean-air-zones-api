package uk.gov.caz.whitelist.service.exception;

public class CsvInvalidCharacterParseException extends IllegalArgumentException {
  @Override
  public String getMessage() {
    return  "Line contains invalid characters.";
  }
}
