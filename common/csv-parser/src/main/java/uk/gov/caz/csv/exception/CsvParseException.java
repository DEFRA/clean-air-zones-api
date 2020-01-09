package uk.gov.caz.csv.exception;

/**
 * An abstract base class for all validation failures upon parsing the CSV file by a subclass of
 * {@link uk.gov.caz.csv.ForwardingCsvParser}. The client of the library should provide their own
 * version of the parser and throw the subclassed exceptions.
 */
public abstract class CsvParseException extends RuntimeException {

  public CsvParseException(String message) {
    super(message);
  }
}
