package uk.gov.caz.accounts.service;

import static org.apache.commons.lang3.StringUtils.trim;

import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;

public class CsvAccountVehicleParser implements ICSVParser {

  static final int MAX_LINE_LENGTH = 30;
  static final int EXPECTED_FIELDS_CNT = 1;

  private static final String MAX_LENGTH_MESSAGE_TEMPLATE =
      "Line is too long (max :" + CsvAccountVehicleParser.MAX_LINE_LENGTH + ", current: %d).";
  private static final String LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE =
      "Line contains %d fields whereas it should " + EXPECTED_FIELDS_CNT + ".";

  private final ICSVParser delegate;

  public CsvAccountVehicleParser(ICSVParser delegate) {
    this.delegate = delegate;
  }

  @Override
  public char getSeparator() {
    return delegate.getSeparator();
  }

  @Override
  public char getQuotechar() {
    return delegate.getQuotechar();
  }

  @Override
  public boolean isPending() {
    return delegate.isPending();
  }

  @Override
  public String[] parseLineMulti(String nextLine) throws IOException {
    checkMaxLineLengthPrecondition(nextLine);

    String[] result = delegate.parseLineMulti(nextLine);
    checkEmptyRow(result);

    checkFieldsCountPostcondition(result);
    return result;
  }

  @Override
  public String[] parseLine(String nextLine) throws IOException {
    return delegate.parseLine(nextLine);
  }

  @Override
  public String parseToLine(String[] values, boolean applyQuotesToAll) {
    return delegate.parseToLine(values, applyQuotesToAll);
  }

  @Override
  public CSVReaderNullFieldIndicator nullFieldIndicator() {
    return delegate.nullFieldIndicator();
  }

  @Override
  public String getPendingText() {
    return delegate.getPendingText();
  }

  @Override
  public void setErrorLocale(Locale locale) {
    delegate.setErrorLocale(locale);
  }

  private void checkEmptyRow(String[] nextLine) {
    boolean isEmptyRow = nextLine == null || trim(String.join("", nextLine)).length() == 0;

    if (isEmptyRow) {
      throw new CsvInvalidCharacterParseException("Line contains invalid characters.");
    }
  }

  private void checkFieldsCountPostcondition(String[] result) {
    if (result.length != EXPECTED_FIELDS_CNT) {
      throw new CsvInvalidFieldsCountException(
          String.format(LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE, result.length)
      );
    }
  }

  private void checkMaxLineLengthPrecondition(String nextLine) {
    int length = nextLine.length();
    if (length > MAX_LINE_LENGTH) {
      throw new CsvMaxLineLengthExceededException(
          String.format(MAX_LENGTH_MESSAGE_TEMPLATE, length)
      );
    }
  }
}
