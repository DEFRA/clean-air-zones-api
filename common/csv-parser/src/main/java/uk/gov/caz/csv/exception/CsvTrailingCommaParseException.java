package uk.gov.caz.csv.exception;

/**
 * An exception that should be thrown when the line being parsed contains a trailing comma.
 */
public class CsvTrailingCommaParseException extends CsvParseException {

  public CsvTrailingCommaParseException(String message) {
    super(message);
  }
}
