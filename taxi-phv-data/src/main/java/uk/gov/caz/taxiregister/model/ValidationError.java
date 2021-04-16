package uk.gov.caz.taxiregister.model;

import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationError implements Comparable<ValidationError> {

  private static final String VALUE_ERROR_TITLE = "Value error";
  private static final String MANDATORY_FIELD_MISSING_ERROR_TITLE = "Mandatory field missing";
  private static final String S3_ERROR_TITLE = "S3 error";
  private static final String INSUFFICIENT_PERMISSIONS_ERROR_TITLE = "Insufficient Permissions";
  private static final String LICENSING_AUTHORITY_UNAVAILABILIY_ERROR_TITLE
      = "Licensing Authority Unavailability";
  private static final String LICENSING_AUTHORITY_UNAVAILABILIY_ERROR
      = "Licence Authority is locked because it is being updated now by another Uploader";
  private static final String REQUEST_PROCESSING_ERROR_TITLE = "Request processing error";

  private static final int EMPTY_LINE_NUMBER = 0;

  private static final ValidationError UNKNOWN_ERROR = new ValidationError(null, "Unknown error",
      "Unknown error occurred while processing registration", EMPTY_LINE_NUMBER);
  private static final ValidationError INTERNAL_ERROR = new ValidationError(null, "Internal error",
      "An internal error occurred while processing registration, please contact the "
          + "system administrator", EMPTY_LINE_NUMBER);
  private static final ValidationError LICENSING_AUTHORITY_UNAVAILABILIY
      = new ValidationError(null, LICENSING_AUTHORITY_UNAVAILABILIY_ERROR_TITLE,
      LICENSING_AUTHORITY_UNAVAILABILIY_ERROR, EMPTY_LINE_NUMBER);

  @ToString.Exclude
  String vrm;
  String title;
  String detail;
  @Getter(AccessLevel.NONE)
  int lineNumber;

  public Optional<Integer> getLineNumber() {
    return lineNumber == EMPTY_LINE_NUMBER ? Optional.empty() : Optional.of(lineNumber);
  }

  public String getDetail() {
    return getLineNumber().map(lineNo -> "Line " + lineNo + ": " + detail).orElse(detail);
  }

  public static ValidationError missingFieldError(String vrm, String detail) {
    return new ValidationError(vrm, MANDATORY_FIELD_MISSING_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  public static ValidationError missingFieldError(String vrm, String detail, int lineNumber) {
    checkLineNumberPrecondition(lineNumber);
    return new ValidationError(vrm, MANDATORY_FIELD_MISSING_ERROR_TITLE, detail, lineNumber);
  }

  public static ValidationError valueError(String vrm, String detail) {
    return new ValidationError(vrm, VALUE_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  public static ValidationError valueError(String vrm, String detail, int lineNumber) {
    checkLineNumberPrecondition(lineNumber);
    return new ValidationError(vrm, VALUE_ERROR_TITLE, detail, lineNumber);
  }

  /**
   * Creates an instance of {@link ValidationError} from an instance of {@link CsvValidationError}.
   *
   * @param csvValidationError A source object that will be mapped into an instance of {@link
   *     ValidationError}.
   * @return An instance of {@link ValidationError}.
   */
  public static ValidationError valueErrorFrom(CsvValidationError csvValidationError) {
    Preconditions.checkNotNull(csvValidationError);
    checkLineNumberPrecondition(csvValidationError.getLineNumber());
    return new ValidationError(null, VALUE_ERROR_TITLE,
        csvValidationError.getDetail(),
        csvValidationError.getLineNumber()
    );
  }

  /**
   * Creates {@link ValidationError} for a case of any AWS S3 error.
   *
   * @param detail Detailed description of the error.
   * @return {@link ValidationError} with proper data.
   */
  public static ValidationError s3Error(String detail) {
    return new ValidationError(null, S3_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  /**
   * Creates {@link ValidationError} for a case of insufficient permissions error.
   *
   * @param detail Detailed description of the error.
   * @return {@link ValidationError} with proper data.
   */
  public static ValidationError insufficientPermissionsError(String detail) {
    return new ValidationError(null, INSUFFICIENT_PERMISSIONS_ERROR_TITLE, detail,
        EMPTY_LINE_NUMBER);
  }

  
  /**
   * Creates {@link ValidationError} for a case of the request processing error.
   *
   * @param detail Detailed description of the error.
   * @return {@link ValidationError} with proper data.
   */
  public static ValidationError requestProcessingError(String detail) {
    return new ValidationError(null, REQUEST_PROCESSING_ERROR_TITLE, detail,
        EMPTY_LINE_NUMBER);
  }

  /**
   * Maps an instance of {@link RegisterJobError} to {@link ValidationError}.
   */
  public static ValidationError from(RegisterJobError registerJobError) {
    return new ValidationError(
        registerJobError.getVrm(),
        registerJobError.getTitle(),
        registerJobError.getDetail(),
        EMPTY_LINE_NUMBER
    );
  }

  public static ValidationError unknown() {
    return UNKNOWN_ERROR;
  }

  public static ValidationError internal() {
    return INTERNAL_ERROR;
  }

  public static ValidationError licensingAuthorityUnavailabilityError() {
    return LICENSING_AUTHORITY_UNAVAILABILIY;
  }
  
  /**
   * Compares this object to {@code other} by comparing objects' line numbers.
   *
   * @param other the object to be compared.
   * @return A negative integer, zero or a positive integer if this object's line number is less
   *     than, equal to, or greater than the specified object's line number. {@link
   *     Integer#MAX_VALUE} is used as the line number if the given object does not contain one.
   */
  @Override
  public int compareTo(ValidationError other) {
    int thisLineNo = getLineNumber().orElse(Integer.MAX_VALUE);
    int otherLineNo = other.getLineNumber().orElse(Integer.MAX_VALUE);
    return Integer.compare(thisLineNo, otherLineNo);
  }

  private static void checkLineNumberPrecondition(int lineNumber) {
    Preconditions.checkArgument(lineNumber > 0, "Line number must be positive");
  }
}
