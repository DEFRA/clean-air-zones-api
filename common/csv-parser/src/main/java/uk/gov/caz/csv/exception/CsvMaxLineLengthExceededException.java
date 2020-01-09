package uk.gov.caz.csv.exception;

/**
 * An exception that should be thrown when the line being parsed is too long.
 */
public class CsvMaxLineLengthExceededException extends CsvParseException {

  public CsvMaxLineLengthExceededException(String message) {
    super(message);
  }
}
