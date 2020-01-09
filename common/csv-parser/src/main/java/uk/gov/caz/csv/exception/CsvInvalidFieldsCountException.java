package uk.gov.caz.csv.exception;

/**
 * An exception that should be thrown when the line being parsed contains invalid number of records.
 */
public class CsvInvalidFieldsCountException extends CsvParseException {

  public CsvInvalidFieldsCountException(String message) {
    super(message);
  }
}
