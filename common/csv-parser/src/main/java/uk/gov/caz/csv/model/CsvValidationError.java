package uk.gov.caz.csv.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Class that represents the validation error that occurred during parsing the csv file.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CsvValidationError {

  private static final String TITLE = "Value error";

  String detail;
  int lineNumber;

  public String getTitle() {
    return TITLE;
  }

  /**
   * Creates an instance of this class.
   */
  public static CsvValidationError with(String detail, int lineNumber) {
    Preconditions.checkArgument(lineNumber > 0, "Line number must be positive");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(detail), "Detail cannot be null or empty");
    return new CsvValidationError(detail, lineNumber);
  }
}
