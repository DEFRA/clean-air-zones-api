package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.csv.exception.CsvParseException;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidBooleanValueException;

@ExtendWith(MockitoExtension.class)
class CsvVehicleDtoParseExceptionResolverTest {

  @InjectMocks
  private CsvVehicleDtoParseExceptionResolver exceptionResolver;

  @Test
  public void shouldThrowNullPointerExceptionWhenNullIsPassed() {
    // given
    CsvParseException exception = null;

    // when
    Throwable throwable = catchThrowable(
        () -> exceptionResolver.resolve(exception, 89));

    // then
    assertThat(throwable).hasMessage("Exception cannot be null")
        .isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = { -12, -1, 0 })
  public void shouldThrowIllegalArgumentExceptionWhenNonPositiveLineNumberIsPassed(
      int lineNumber) {
    // given
    CsvParseException exception = new CsvInvalidFieldsCountException("");

    // when
    Throwable throwable = catchThrowable(
        () -> exceptionResolver.resolve(exception, lineNumber));

    // then
    assertThat(throwable).hasMessage("Line number must be positive")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldResolveInvalidFieldsCountError() {
    // given
    CsvInvalidFieldsCountException exception = new CsvInvalidFieldsCountException(
        "");
    int lineNumber = 76;

    // when
    Optional<CsvValidationError> errorOptional = exceptionResolver
        .resolve(exception, lineNumber);

    assertThat(errorOptional).isPresent();
    assertThat(errorOptional).hasValueSatisfying(csvValidationError -> {
      assertThat(csvValidationError.getDetail()).isEqualTo(
          CsvVehicleDtoParseExceptionResolver.LINE_INVALID_FIELDS_COUNT_MESSAGE);
      assertThat(csvValidationError.getLineNumber()).isEqualTo(lineNumber);
    });
  }

  @Test
  public void shouldResolveInvalidFormatError() {
    // given
    CsvInvalidCharacterParseException exception = new CsvInvalidCharacterParseException(
        "");
    int lineNumber = 82;

    // when
    Optional<CsvValidationError> errorOptional = exceptionResolver
        .resolve(exception, lineNumber);

    assertThat(errorOptional).isPresent();
    assertThat(errorOptional).hasValueSatisfying(csvValidationError -> {
      assertThat(csvValidationError.getDetail()).isEqualTo(
          CsvVehicleDtoParseExceptionResolver.LINE_INVALID_FORMAT_MESSAGE);
      assertThat(csvValidationError.getLineNumber()).isEqualTo(lineNumber);
    });
  }

  @Test
  public void shouldResolveTooLongLineError() {
    // given
    CsvMaxLineLengthExceededException exception = new CsvMaxLineLengthExceededException(
        "");
    int lineNumber = 45;

    // when
    Optional<CsvValidationError> errorOptional = exceptionResolver
        .resolve(exception, lineNumber);

    assertThat(errorOptional).isPresent();
    assertThat(errorOptional).hasValueSatisfying(csvValidationError -> {
      assertThat(csvValidationError.getDetail())
          .isEqualTo(CsvVehicleDtoParseExceptionResolver.LINE_TOO_LONG_MESSAGE);
      assertThat(csvValidationError.getLineNumber()).isEqualTo(lineNumber);
    });
  }

  @Test
  public void shouldResolveInvalidBooleanError() {
    // given
    CsvInvalidBooleanValueException exception = new CsvInvalidBooleanValueException(
        "");
    int lineNumber = 68;

    // when
    Optional<CsvValidationError> errorOptional = exceptionResolver
        .resolve(exception, lineNumber);

    assertThat(errorOptional).isPresent();
    assertThat(errorOptional).hasValueSatisfying(csvValidationError -> {
      assertThat(csvValidationError.getDetail()).isEqualTo(
          CsvVehicleDtoParseExceptionResolver.INVALID_BOOLEAN_VALUE_MESSAGE);
      assertThat(csvValidationError.getLineNumber()).isEqualTo(lineNumber);
    });
  }

  @Test
  public void shouldReturnEmptyOptionalForNotSupportedException() {
    // given
    CsvParseException exception = new CsvParseException("") {
    };
    int lineNumber = 43;

    // when
    Optional<CsvValidationError> errorOptional = exceptionResolver
        .resolve(exception, lineNumber);

    assertThat(errorOptional).isEmpty();
  }
}