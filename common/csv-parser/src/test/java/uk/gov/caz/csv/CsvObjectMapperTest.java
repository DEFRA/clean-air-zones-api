package uk.gov.caz.csv;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.google.common.base.Charsets;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.LineValidator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvParseException;
import uk.gov.caz.csv.model.CsvParseResult;
import uk.gov.caz.csv.model.CsvValidationError;

@ExtendWith(MockitoExtension.class)
class CsvObjectMapperTest {

  @Value
  private static class ValueClass {

    String text;
    int lineNumber;
  }

  private static class CsvObjectMapperImpl extends CsvObjectMapper<ValueClass> {

    CsvObjectMapperImpl(ICSVParser csvParser, int maxErrorsCount,
        CsvParseExceptionResolver csvParseExceptionResolver) {
      super(csvParser, maxErrorsCount, csvParseExceptionResolver);
    }

    CsvObjectMapperImpl(ICSVParser csvParser, CsvParseExceptionResolver csvParseExceptionResolver) {
      super(csvParser, csvParseExceptionResolver);
    }

    CsvObjectMapperImpl(ICSVParser csvParser, int maxErrorsCount,
        CsvParseExceptionResolver csvParseExceptionResolver,
        Consumer<CSVReaderBuilder> csvReaderModifier) {
      super(csvParser, maxErrorsCount, csvParseExceptionResolver, csvReaderModifier);
    }

    @Override
    public ValueClass mapToObject(String[] fields, int lineNo) {
      return new ValueClass(fields[0], lineNo);
    }
  }

  private static final int ANY_MAX_ERROR_COUNT = 2;

  @Mock
  private ICSVParser parser;

  @Mock
  private CsvParseExceptionResolver exceptionResolver;

  private CsvObjectMapper<ValueClass> csvObjectMapper;

  @BeforeEach
  public void setUp() {
    csvObjectMapper = new CsvObjectMapperImpl(parser, ANY_MAX_ERROR_COUNT, exceptionResolver);
  }

  @ParameterizedTest
  @ValueSource(ints = {-10, 0})
  public void shouldThrowIllegalArgumentExceptionWhenMaxErrorIsNotPositiveOrMinusOne(
      int maxErrorCount) {
    // when
    Throwable throwable = catchThrowable(() ->
        csvObjectMapper = new CsvObjectMapperImpl(parser, maxErrorCount, exceptionResolver)
    );

    // then
    then(throwable)
        .hasMessageStartingWith("Max errors count must be positive")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowNullPointerExceptionIfCsvReaderModifierIsNull() {
    // given
    Consumer<CSVReaderBuilder> builderConsumer = null;

    // when
    Throwable throwable = catchThrowable(() ->
        csvObjectMapper = new CsvObjectMapperImpl(parser, 10, exceptionResolver, builderConsumer)
    );

    // then
    then(throwable)
        .hasMessage("csvReaderModifier cannot be null")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowNullPointerExceptionIfInputStreamIsNull() {
    // given
    InputStream inputStream = null;

    // when
    Throwable throwable = catchThrowable(() -> csvObjectMapper.read(inputStream));

    // then
    then(throwable)
        .hasMessage("Input stream cannot be null")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowIllegalStateExceptionIfOpenCSVReportsInvalidLine() {
    // given
    csvObjectMapper = getCsvObjectMapperWithCustomLineValidator();

    // when
    Throwable throwable = catchThrowable(() -> csvObjectMapper.read(toInputStream(
        createLines(1))));

    // then
    then(throwable)
        .hasMessage("This should never happen as we do not use custom validators in opencsv")
        .isInstanceOf(IllegalStateException.class);
  }

  @Nested
  class WhenThereAreParseErrors {

    @Test
    public void shouldIncrementLineNumberWhenInitialAreSkipped() throws IOException {
      // given
      int skipLines = 2;
      mockParsingResultsWithErrors(2);
      csvObjectMapper = new CsvObjectMapperImpl(parser, 10, exceptionResolver,
          csvReaderBuilder -> csvReaderBuilder.withSkipLines(skipLines));

      // when
      CsvParseResult<ValueClass> result = csvObjectMapper
          .read(getFileInputStream("data/csv/data-with-header.csv"));

      // then
      then(result.getValidationErrors()).hasSize(1);
      then(result.getValidationErrors().iterator().next())
          .extracting(CsvValidationError::getLineNumber)
          .isEqualTo(4);
    }
  }

  @Nested
  class WhenThereAreNoParseErrors {

    @Value
    class TestClass {

      private String field1;
      private String field;
    }

    @Test
    public void shouldParseFile() throws IOException {
      // given
      int numberOfLines = 13;
      mockParsingResultsWithoutErrors(numberOfLines);

      // when
      CsvParseResult<ValueClass> result = csvObjectMapper.read(toInputStream(
          createLines(numberOfLines)));

      // then
      then(result.getValidationErrors()).isEmpty();
      then(result.getObjects()).hasSize(numberOfLines);
    }

    @Test
    public void shouldParseFileWithFieldsWithEmptyValues() throws IOException {
      // given
      CsvObjectMapper<TestClass> csvObjectMapper = new CsvObjectMapper<TestClass>(new CSVParser(),
          ANY_MAX_ERROR_COUNT,
          (exception, lineNumber) -> Optional.empty()) {
        @Override
        public TestClass mapToObject(String[] fields, int lineNo) {
          return new TestClass(fields[0], fields[1]);
        }
      };

      InputStream inputStream = getFileInputStream("data/csv/2-columns-first-empty.csv");

      // when
      CsvParseResult<TestClass> result = csvObjectMapper.read(inputStream);

      // then
      then(result.getValidationErrors()).isEmpty();
      then(result.getObjects()).hasSize(1);
      then(result.getObjects().get(0)).isEqualTo(new TestClass("", "BB444CC"));
    }
  }

  @Nested
  class WhenErrorsSizeExceededMaxErrorCount {

    @Test
    public void shouldStopParsingFile() throws IOException {
      // given
      int localMaxErrorCount = 2;
      int numberOfLines = localMaxErrorCount + 15;
      csvObjectMapper = new CsvObjectMapperImpl(parser, localMaxErrorCount, exceptionResolver);
      mockParsingResultsWithErrors(numberOfLines);

      // when
      CsvParseResult<ValueClass> result = csvObjectMapper.read(toInputStream(
          createLines(numberOfLines)));

      // then
      then(result.getValidationErrors()).hasSize(localMaxErrorCount);
    }

    @Test
    public void shouldStopParsingFileWhenNextLineAfterStopContainsError() throws IOException {
      // given
      int localMaxErrorCount = 1;
      int numberOfLines = localMaxErrorCount + 2;
      csvObjectMapper = new CsvObjectMapperImpl(parser, localMaxErrorCount, exceptionResolver);
      mockParsingResultsWithErrors(numberOfLines - 1)
          .willThrow(new CsvInvalidFieldsCountException("Invalid fields count"));

      // when
      CsvParseResult<ValueClass> result = csvObjectMapper.read(toInputStream(
          createLines(numberOfLines)));

      // then
      then(result.getValidationErrors()).hasSize(localMaxErrorCount);
    }
  }

  @Nested
  class WhenErrorsSizeIsEqualToMinusOne {

    @Test
    public void shouldNotStopParsingFile() throws IOException {
      // given
      int numberOfLines = 15;
      csvObjectMapper = new CsvObjectMapperImpl(parser, exceptionResolver);
      mockParsingResultsWithErrors(numberOfLines);

      // when
      CsvParseResult<ValueClass> result = csvObjectMapper.read(toInputStream(
          createLines(numberOfLines)));

      // then
      then(result.getValidationErrors()).hasSize(7);
    }
  }

  private String getAsciiCharacter(int i) {
    return String.valueOf(Character.forDigit(i + 10 - 1, Character.MAX_RADIX));
  }

  private String createLines(int numberOfLines) {
    return IntStream.rangeClosed(1, numberOfLines)
        .mapToObj(this::getAsciiCharacter)
        .collect(Collectors.joining("\n"));
  }

  private void mockExceptionResolver() {
    given(exceptionResolver.resolve(any(CsvParseException.class), anyInt())).willAnswer(answer -> {
      int lineNumber = answer.getArgument(1);
      return Optional.of(CsvValidationError.with("parse error", lineNumber));
    });
  }

  private BDDMyOngoingStubbing<String[]> mockParsingResultsWithoutErrors(int numberOfLines)
      throws IOException {
    BDDMyOngoingStubbing<String[]> stubbing = given(parser.parseLineMulti(anyString()));
    for (int i = 0; i < numberOfLines; i++) {
      stubbing = stubbing.willReturn(new String[]{getAsciiCharacter(i + 1)});
    }
    return stubbing;
  }

  private BDDMyOngoingStubbing<String[]> mockParsingResultsWithErrors(int numberOfLines)
      throws IOException {
    BDDMyOngoingStubbing<String[]> stubbing = given(parser.parseLineMulti(anyString()));
    for (int i = 0; i < numberOfLines; i++) {
      stubbing = i % 2 == 0
          ? stubbing.willReturn(new String[]{getAsciiCharacter(i + 1)})
          : stubbing.willThrow(new CsvInvalidCharacterParseException("Invalid character"));
    }
    mockExceptionResolver();
    return stubbing;
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }

  @SneakyThrows
  private FileInputStream getFileInputStream(String filename) {
    return new FileInputStream(
        new File(getClass().getClassLoader().getResource(filename).toURI())
    );
  }

  private CsvObjectMapperImpl getCsvObjectMapperWithCustomLineValidator() {
    return new CsvObjectMapperImpl(parser, Integer.MAX_VALUE, exceptionResolver) {
      @Override
      protected CSVReader createReader(InputStream inputStream) {
        CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(
            new InputStreamReader(inputStream));
        csvReaderBuilder.withCSVParser(parser);
        csvReaderBuilder.withLineValidator(new LineValidator() {
          @Override
          public boolean isValid(String line) {
            return true;
          }

          @Override
          public void validate(String line) throws CsvValidationException {
            throw new CsvValidationException("invalid line");
          }
        });
        return csvReaderBuilder.build();
      }
    };
  }
}