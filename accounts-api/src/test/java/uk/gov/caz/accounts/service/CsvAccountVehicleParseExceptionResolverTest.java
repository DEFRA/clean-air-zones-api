package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvParseException;

@ExtendWith(MockitoExtension.class)
class CsvAccountVehicleParseExceptionResolverTest {

  private CsvAccountVehicleParseExceptionResolver resolver =
      new CsvAccountVehicleParseExceptionResolver();

  @ParameterizedTest
  @ValueSource(ints = {-12, -1, 0})
  public void shouldThrowIllegalArgumentExceptionWhenNonPositiveLineNumberIsPassed(
      int lineNumber) {
    // given
    CsvParseException exception = new CsvInvalidFieldsCountException("");

    // when
    Throwable throwable = catchThrowable(
        () -> resolver.resolve(exception, lineNumber));

    // then
    assertThat(throwable).hasMessage("Line number must be positive")
        .isInstanceOf(IllegalArgumentException.class);
  }

}