package uk.gov.caz.taxiregister.service.exception;

import uk.gov.caz.csv.exception.CsvParseException;

public class CsvInvalidBooleanValueException extends CsvParseException {

  public CsvInvalidBooleanValueException(String message) {
    super(message);
  }
}
