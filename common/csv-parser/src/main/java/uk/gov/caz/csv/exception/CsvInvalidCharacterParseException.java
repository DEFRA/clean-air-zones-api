package uk.gov.caz.csv.exception;

/**
 * An exception that should be thrown when the line being parsed contains not allowed character(s).
 */
public class CsvInvalidCharacterParseException extends CsvParseException {

  public CsvInvalidCharacterParseException(String message) {
    super(message);
  }
}
