package uk.gov.caz.csv;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.UnaryOperator;

/**
 * A class that replaces the validation error message with the information about
 * potentially included header row.
 */
public class CsvHeaderRowValidationMessageReplacer {

  @VisibleForTesting
  static final String PRESENT_HEADER_MESSAGE_REPLACEMENT = "Header information should not "
      + "be included";

  private static final UnaryOperator<String> NO_OP = UnaryOperator.identity();
  private static final UnaryOperator<String> CSV_HEADER_PRESENT_REPLACER =
      message -> PRESENT_HEADER_MESSAGE_REPLACEMENT;

  /**
   * Replaces {@code message} with the information about the possibly included header row if {@code
   * lineNumber == 1}, otherwise {@code message} is returned.
   */
  public String replaceMessageWithHeaderRowErrorIfApplicable(String message, int lineNumber) {
    return modifier(lineNumber).apply(message);
  }

  private UnaryOperator<String> modifier(int lineNumber) {
    return isFirstLine(lineNumber) ? CSV_HEADER_PRESENT_REPLACER : NO_OP;
  }

  private boolean isFirstLine(int lineNumber) {
    return lineNumber == 1;
  }
}
