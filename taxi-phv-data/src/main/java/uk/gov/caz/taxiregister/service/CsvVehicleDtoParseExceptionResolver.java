package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import uk.gov.caz.csv.CsvParseExceptionResolver;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.csv.exception.CsvParseException;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidBooleanValueException;

/**
 * A class which maps subclass of {@link CsvParseException} to
 * {@link CsvValidationError}.
 */
@Component
public class CsvVehicleDtoParseExceptionResolver
    implements CsvParseExceptionResolver {

  @VisibleForTesting
  static final String INVALID_BOOLEAN_VALUE_MESSAGE = "Invalid wheelchair accessible value."
      + " Can only be True or False";

  @VisibleForTesting
  static final String LINE_TOO_LONG_MESSAGE = "Line is too long";

  @VisibleForTesting
  static final String LINE_INVALID_FORMAT_MESSAGE = "Invalid character or empty row detected";

  @VisibleForTesting
  static final String LINE_INVALID_FIELDS_COUNT_MESSAGE =
      "Record doesn't match the data rule specifications";

  private final Map<Class<? extends CsvParseException>, Function<Integer, CsvValidationError>>
      exceptionToError;

  /**
   * Creates an instance of this class.
   */
  public CsvVehicleDtoParseExceptionResolver() {
    this.exceptionToError = mapping();
  }

  /**
   * Maps the passed {@code exception} with the {@code lineNumber} to
   * {@link CsvValidationError}. If a given subclass of
   * {@link CsvParseException} is not supported, {@link Optional#empty()} is
   * returned.
   *
   * @param exception  A subclass of {@link CsvParseException}. Cannot be null.
   * @param lineNumber The line number of the input csv file which contained a
   *     parse error that caused the exception.
   * @return {@link Optional#empty()} if this class does not support the mapping
   *     of a given {@code exception}, an optional containing {@link CsvValidationError}
   *     otherwise.
   */
  @Override
  public Optional<CsvValidationError> resolve(CsvParseException exception,
      int lineNumber) {
    Preconditions.checkNotNull(exception, "Exception cannot be null");
    Preconditions.checkArgument(lineNumber > 0, "Line number must be positive");

    return Optional.ofNullable(exceptionToError.get(exception.getClass()))
        .map(function -> function.apply(lineNumber));
  }

  /**
   * Creates an instance of {@link CsvValidationError} with invalid fields count
   * error.
   *
   * @param lineNo The line number of the input csv file which contained a parse
   *     error that caused the exception.
   * @return An instance of {@link CsvValidationError} with invalid fields count
   *     error.
   */
  private CsvValidationError createInvalidFieldsCountError(int lineNo) {
    return CsvValidationError.with(LINE_INVALID_FIELDS_COUNT_MESSAGE, lineNo);
  }

  /**
   * Creates an instance of {@link CsvValidationError} with maximum line length
   * exceeded error.
   *
   * @param lineNo The line number of the input csv file which contained a parse
   *     error that caused the exception.
   * @return An instance of {@link CsvValidationError} with maximum line length
   *     exceeded error.
   */
  private CsvValidationError createMaximumLineLengthExceededError(int lineNo) {
    return CsvValidationError.with(LINE_TOO_LONG_MESSAGE, lineNo);
  }

  /**
   * Creates an instance of {@link CsvValidationError} with parse validation
   * error.
   *
   * @param lineNo The line number of the input csv file which contained a parse
   *     error that caused the exception.
   * @return An instance of {@link CsvValidationError} with parse validation
   *     error.
   */
  private CsvValidationError createParseValidationError(int lineNo) {
    return CsvValidationError.with(LINE_INVALID_FORMAT_MESSAGE, lineNo);
  }

  /**
   * Creates an instance of {@link CsvValidationError} with invalid format of a
   * boolean value error.
   *
   * @param lineNo The line number of the input csv file which contained a parse
   *     error that caused the exception.
   * @return An instance of {@link CsvValidationError} with invalid format of a
   *     boolean value error.
   */
  private CsvValidationError createInvalidBooleanFlagValueError(int lineNo) {
    return CsvValidationError.with(INVALID_BOOLEAN_VALUE_MESSAGE, lineNo);
  }

  /**
   * Returns a map between subclasses of {@link CsvParseException} and
   * references {@link CsvValidationError} factory methods.
   */
  private Map<Class<? extends CsvParseException>, Function<Integer, CsvValidationError>> mapping() {
    return ImmutableMap.of(CsvInvalidBooleanValueException.class,
        this::createInvalidBooleanFlagValueError,
        CsvInvalidFieldsCountException.class,
        this::createInvalidFieldsCountError,
        CsvMaxLineLengthExceededException.class,
        this::createMaximumLineLengthExceededError,
        CsvInvalidCharacterParseException.class,
        this::createParseValidationError);
  }
}
