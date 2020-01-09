package uk.gov.caz.taxiregister.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import java.util.regex.Pattern;
import uk.gov.caz.csv.ForwardingCsvParser;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidBooleanValueException;

/**
 * A taxi/phv licence parser. Apart from parsing the given line of an input file, it validates it by
 * throwing dedicated exceptions when necessary.
 */
public class CsvLicenceParser extends ForwardingCsvParser {

  @VisibleForTesting
  static final int MAX_LINE_LENGTH = 210;

  private static final int EXPECTED_FIELDS_CNT = 7;

  private static final String INVALID_BOOLEAN_VALUE_MESSAGE_TEMPLATE = "Invalid value of "
      + "a boolean flag (\"true\" or \"false\" in any capitalization), actual: '%s'";
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
    checkBooleanFlagFormat(result);
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
   * Verifies whether the format of the optional boolean value is correct.
   *
   * @param result A {@link String} array containing records of the parsed line.
   * @throws CsvInvalidBooleanValueException if the format is incorrect.
   */
  private void checkBooleanFlagFormat(String[] result) {
    String lastValue;
    if ((lastValue = Strings.emptyToNull(lastValueOf(result))) == null) {
      return;
    }
    boolean isTrueOrFalseInAnyCapitalization = lastValue.equalsIgnoreCase(TRUE.toString())
        || lastValue.equalsIgnoreCase(FALSE.toString());

    if (!isTrueOrFalseInAnyCapitalization) {
      throw new CsvInvalidBooleanValueException(
          String.format(INVALID_BOOLEAN_VALUE_MESSAGE_TEMPLATE, lastValue)
      );
    }
  }

  /**
   * Returns the last value of the passed array. Precondition: the array must be non-empty.
   *
   * @param result An array whose last element is to be obtained.
   * @return The last element of the input array.
   */
  private String lastValueOf(String[] result) {
    return result[result.length - 1];
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
