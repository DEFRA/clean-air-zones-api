package uk.gov.caz.whitelist.service;

import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;
import uk.gov.caz.whitelist.service.exception.CsvBlankRowException;
import uk.gov.caz.whitelist.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.whitelist.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.whitelist.service.exception.CsvMaxLineLengthExceededException;

public class CsvWhitelistedVehicleParser implements ICSVParser {

  static final int MAX_LINE_LENGTH = 140;
  static final int EXPECTED_FIELDS_CNT = 5;

  private static final String MAX_LENGTH_MESSAGE_TEMPLATE =
      "Line is too long (max :" + CsvWhitelistedVehicleParser.MAX_LINE_LENGTH + ", current: %d).";
  private static final String LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE =
      "Line contains %d fields whereas it should " + + EXPECTED_FIELDS_CNT + ".";

  private static final String REGEX = "^[\\w &,'\"\\-().*/%!+:;=?@\\[\\]^{}~\\\\]+$";
  private static final Pattern ALLOWABLE_CHARACTERS = Pattern.compile(REGEX);

  private final ICSVParser delegate;

  public CsvWhitelistedVehicleParser(ICSVParser delegate) {
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
    checkAllowableCharactersPrecondition(nextLine);

    String[] result = delegate.parseLineMulti(nextLine);

    checkNotBlankRowPostcondition(result);
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


  private void checkNotBlankRowPostcondition(String[] result) {
    if (areAllFieldsEmpty(result)) {
      throw new CsvBlankRowException();
    }
  }

  private boolean areAllFieldsEmpty(String[] result) {
    return Stream.of(result)
        .map(StringUtils::trimAllWhitespace)
        .map(Strings::emptyToNull)
        .allMatch(Objects::isNull);
  }

  private void checkFieldsCountPostcondition(String[] result) {
    if (result.length != EXPECTED_FIELDS_CNT) {
      throw new CsvInvalidFieldsCountException(
          result.length,
          String.format(LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE, result.length)
      );
    }
  }

  private void checkMaxLineLengthPrecondition(String nextLine) {
    int length = nextLine.length();
    if (length > MAX_LINE_LENGTH) {
      throw new CsvMaxLineLengthExceededException(
          String.format(MAX_LENGTH_MESSAGE_TEMPLATE, length),
          length
      );
    }
  }

  private void checkAllowableCharactersPrecondition(String nextLine) {
    if (!ALLOWABLE_CHARACTERS.matcher(nextLine).matches()) {
      throw new CsvInvalidCharacterParseException();
    }
  }
}
