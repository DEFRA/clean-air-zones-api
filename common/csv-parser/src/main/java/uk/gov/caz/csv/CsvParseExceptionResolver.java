package uk.gov.caz.csv;

import java.util.Optional;
import uk.gov.caz.csv.exception.CsvParseException;
import uk.gov.caz.csv.model.CsvValidationError;

/**
 * An interface for resolving csv parse errors.
 */
public interface CsvParseExceptionResolver {

  /**
   * Maps {@code exception} to an instance of {@link CsvValidationError}. If no such mapping is
   * supported, the {@link Optional#empty()} should be returned.
   *
   * @param exception An instance of exception that will be mapped into an instance of {@link
   *     CsvValidationError}.
   * @param lineNumber An integer that indicates the line number of a file where validation
   *     failed.
   * @return An {@link Optional} containing an instance of {@link CsvValidationError} with passed
   *     {@code lineNumber} or an empty {@link Optional} if the mapping is not supported.
   */
  Optional<CsvValidationError> resolve(CsvParseException exception, int lineNumber);
}
