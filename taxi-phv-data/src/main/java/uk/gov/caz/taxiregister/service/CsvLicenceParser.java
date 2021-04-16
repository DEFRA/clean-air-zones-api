package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.opencsv.ICSVParser;
import java.util.regex.Pattern;
import uk.gov.caz.csv.ForwardingCsvParser;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;

/**
 * A taxi/phv licence parser. Apart from parsing the given line of an input file, it validates it by
 * throwing dedicated exceptions when necessary.
 */
public class CsvLicenceParser extends ForwardingCsvParser {

  @VisibleForTesting
  static final int MAX_LINE_LENGTH = 210;

  private static final int EXPECTED_FIELDS_CNT = 7;

  private static final String MAX_LENGTH_MESSAGE_TEMPLATE =
      "Line is too long (max :" + CsvLicenceParser.MAX_LINE_LENGTH + ", current: %d)";
  private static final String LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE = "Line contains %d fields "
      + "whereas it should " + EXPECTED_FIELDS_CNT;

  private static final String REGEX = "^[\\w &,'\"\\-().*/%!+:;=?@\\[\\]^{}~]+$";
  private static final Pattern ALLOWABLE_CHARACTERS = Pattern.compile(REGEX);

  public CsvLicenceParser(ICSVParser delegate) {
    super(delegate);
  }

  /**
   * Verifies whether the parsed line (stored as an array of strings in {@code result}) contains the
   * expected number of records and the format of the optional boolean value is correct.
   *
   * @param result A {@link String} array containing records of the parsed line.
   */
  @Override
  protected final void afterParseLine(String[] result) {
    checkFieldsCountPostcondition(result);
  }

  /**
   * Verifies whether the line which to be parsed does not exceed the max length limit and contains
   * allowable characters.
   *
   * @param nextLine The line which is to be parsed.
   */
  @Override
  protected final void beforeParseLine(String nextLine) {
    checkMaxLineLengthPrecondition(nextLine);
    checkAllowableCharactersPrecondition(nextLine);
  }

  /**
   * Verifies whether the number of records of the parsed line is correct.
   *
   * @param result A {@link String} array containing records of the parsed line.
   * @throws CsvInvalidFieldsCountException if the count is invalid.
   */
  private void checkFieldsCountPostcondition(String[] result) {
    if (result.length != EXPECTED_FIELDS_CNT) {
      throw new CsvInvalidFieldsCountException(
          String.format(LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE, result.length)
      );
    }
  }

  /**
   * Verifies whether the line which to be parsed does not exceed the max length limit.
   *
   * @param nextLine The line which is to be parsed.
   * @throws CsvMaxLineLengthExceededException if the length has been exceeded.
   */
  private void checkMaxLineLengthPrecondition(String nextLine) {
    int length = nextLine.length();
    if (length > MAX_LINE_LENGTH) {
      throw new CsvMaxLineLengthExceededException(
          String.format(MAX_LENGTH_MESSAGE_TEMPLATE, length)
      );
    }
  }

  /**
   * Verifies whether the line which to be parsed contains only allowable characters.
   *
   * @param nextLine The line which is to be parsed.
   * @throws CsvInvalidCharacterParseException if the line contains not allowed characters.
   */
  private void checkAllowableCharactersPrecondition(String nextLine) {
    if (!ALLOWABLE_CHARACTERS.matcher(nextLine).matches()) {
      throw new CsvInvalidCharacterParseException("Invalid format of the line (regex format)");
    }
  }
}
