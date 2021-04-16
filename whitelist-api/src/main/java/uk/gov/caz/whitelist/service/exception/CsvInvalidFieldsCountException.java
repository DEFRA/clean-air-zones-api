package uk.gov.caz.whitelist.service.exception;

public class CsvInvalidFieldsCountException extends IllegalArgumentException {

  private final int fieldsCount;

  public CsvInvalidFieldsCountException(int fieldsCount, String message) {
    super(message);
    this.fieldsCount = fieldsCount;
  }

  public int getFieldsCount() {
    return fieldsCount;
  }
}
