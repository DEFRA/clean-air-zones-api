package uk.gov.caz.csv;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.BOMInputStream;
import uk.gov.caz.csv.exception.CsvParseException;
import uk.gov.caz.csv.model.CsvParseResult;
import uk.gov.caz.csv.model.CsvValidationError;

/**
 * A class that provides methods to map an {@link InputStream} of a CSV data to a collection of type
 * {@code T} wrapped in {@link CsvParseResult}.
 */
@Slf4j
public abstract class CsvObjectMapper<T> {

  private final ICSVParser csvParser;
  private final int maxErrorsCount;
  private final CsvParseExceptionResolver csvParseExceptionResolver;
  private final Consumer<CSVReaderBuilder> csvReaderModifier;

  /**
   * Creates an instance of this class with the default noop {@code csvReaderModifier}.
   */
  public CsvObjectMapper(ICSVParser csvParser, int maxErrorsCount,
      CsvParseExceptionResolver csvParseExceptionResolver) {
    this(csvParser, maxErrorsCount, csvParseExceptionResolver, builder -> {
    });
  }

  /**
   * Creates an instance of this class with {@code maxErrorsCount} = -1 and the default noop {@code
   * csvReaderModifier}.
   */
  public CsvObjectMapper(ICSVParser csvParser,
      CsvParseExceptionResolver csvParseExceptionResolver) {
    this(csvParser, -1, csvParseExceptionResolver, builder -> {
    });
  }

  /**
   * Creates an instance of this class with a dedicated {@code csvReaderModifier}.
   */
  public CsvObjectMapper(ICSVParser csvParser, int maxErrorsCount,
      CsvParseExceptionResolver csvParseExceptionResolver,
      Consumer<CSVReaderBuilder> csvReaderModifier) {
    Preconditions.checkNotNull(csvReaderModifier, "csvReaderModifier cannot be null");
    Preconditions.checkArgument(maxErrorsCount == -1 || maxErrorsCount > 0,
        "Max errors count must be positive or equal to -1, current value: %s", maxErrorsCount);

    this.csvParseExceptionResolver = csvParseExceptionResolver;
    this.csvParser = csvParser;
    this.maxErrorsCount = maxErrorsCount;
    this.csvReaderModifier = csvReaderModifier;
  }

  /**
   * Reads data from {@code inputStream} and maps it to a {@link CsvParseResult}. The {@code
   * inputStream} *MUST* contain data in CSV format. The {@code inputStream} *MUST* be closed by the
   * client code to avoid memory leaks.
   *
   * @param inputStream A stream which contains data in CSV format.
   * @return An instance of {@link CsvParseResult}.
   * @throws NullPointerException if {@code inputStream} is null.
   */
  public CsvParseResult<T> read(InputStream inputStream) throws IOException {
    Preconditions.checkNotNull(inputStream, "Input stream cannot be null");
    
    inputStream = new BOMInputStream(inputStream);

    ImmutableList.Builder<T> mappedObjects = ImmutableList.builder();
    LinkedList<CsvValidationError> errors = Lists.newLinkedList();
    CSVReader reader = createReader(inputStream);

    String[] fields;
    int lineNo = 1 + reader.getSkipLines();
    while ((maxErrorsCount == -1 || errors.size() < maxErrorsCount)
        && (fields = readLine(reader, errors, lineNo)) != null) {
      if (fields.length == 0) {
        log.trace("Validation error on line {}, skipping it", lineNo);
      } else {
        if (lineNo == 1) {
          fields[0] = fields[0].replace("\uFEFF", "");
        }
        T mappedObject = mapToObject(fields, lineNo);
        mappedObjects.add(mappedObject);
        log.debug("Object read successfully");
      }
      lineNo += 1;
    }
    logParsingEndReason(errors);

    return new CsvParseResult<>(mappedObjects.build(), Collections.unmodifiableList(errors));
  }

  /**
   * Maps {@code fields} to an instance of type {@code T}. To be implemented in a subclass.
   *
   * @param fields An array of strings which represents the attributes of type {@code T}.
   * @param lineNo Line number in the source file from which data in {@code fields} comes from.
   * @return An instance of {@code T}.
   */
  public abstract T mapToObject(String[] fields, int lineNo);

  /**
   * Creates an instance of {@link CSVReader} based on passed {@link InputStream}.
   *
   * @param inputStream An input stream of a CSV file.
   * @return An instance of {@link CSVReader} created for {@code inputStream}.
   */
  @VisibleForTesting
  protected CSVReader createReader(InputStream inputStream) {
    CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new InputStreamReader(inputStream))
        .withCSVParser(csvParser);
    csvReaderModifier.accept(csvReaderBuilder);
    return csvReaderBuilder.build();
  }

  /**
   * Reads line and returns null if end of the stream, empty array if validation error, non-empty
   * array on success.
   */
  private String[] readLine(CSVReader reader, LinkedList<CsvValidationError> errors, int lineNumber)
      throws IOException {
    try {
      return reader.readNext();
    } catch (CsvParseException e) {
      log.debug("CSV parse exception on line {}: {}", lineNumber, e.getMessage());
      csvParseExceptionResolver.resolve(e, lineNumber).ifPresent(csvValidationError -> {
        log.info("Validation error detected: {}", csvValidationError.getDetail());
        errors.add(csvValidationError);
      });
      return new String[0];
    } catch (CsvValidationException e) {
      log.error("This should never happen as we do not use custom validators in opencsv", e);
      throw new IllegalStateException(
          "This should never happen as we do not use custom validators in opencsv", e);
    }
  }

  /**
   * Logs the reason of finishing parsing.
   *
   * @param errors A list of parse errors.
   */
  private void logParsingEndReason(LinkedList<CsvValidationError> errors) {
    if (errorsMaxCountReached(errors)) {
      log.info("Finished parsing the input file: error max count ({}) reached", maxErrorsCount);
    } else {
      log.info("Finished parsing the input file: reached EOF");
    }
  }

  /**
   * Returns a boolean flag indicating whether the {@code errors} list reached the maximum allowed
   * capacity.
   *
   * @param errors A list of parse errors.
   * @return true if the size of {@code errors} reached or exceeded the maximum allowed, false
   *     otherwise.
   */
  private boolean errorsMaxCountReached(LinkedList<CsvValidationError> errors) {
    return maxErrorsCount != - 1 && errors.size() >= maxErrorsCount;
  }
}
