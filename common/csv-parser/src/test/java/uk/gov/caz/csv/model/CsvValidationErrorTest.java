package uk.gov.caz.csv.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvValidationErrorTest {

  @Nested
  class WhenValidationFails {
    @ParameterizedTest
    @ValueSource(ints = {-100, -10, -1, 0})
    public void shouldThrowIllegalArgumentExceptionWhenLineNumberIsNonPositive(int lineNumber) {
      // when
      Throwable throwable = catchThrowable(() -> CsvValidationError.with("detail", lineNumber));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenDetailIsNull() {
      // given
      int lineNumber = 1;
      String detail = null;

      // when
      Throwable throwable = catchThrowable(() -> CsvValidationError.with(detail, lineNumber));

      // then
      assertThat(throwable)
          .hasMessage("Detail cannot be null or empty")
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenDetailIsEmpty() {
      // given
      int lineNumber = 1;
      String detail = "";

      // when
      Throwable throwable = catchThrowable(() -> CsvValidationError.with(detail, lineNumber));

      // then
      assertThat(throwable)
          .hasMessage("Detail cannot be null or empty")
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void shouldCreateObjectWhenAllParamsAreValid() {
    // given
    int lineNumber = 1;
    String detail = "valid detail";
    // when
    CsvValidationError csvValidationError = CsvValidationError.with(detail, lineNumber);

    // then
    assertThat(csvValidationError.getDetail()).isEqualTo(detail);
    assertThat(csvValidationError.getTitle()).isEqualTo("Value error");
    assertThat(csvValidationError.getLineNumber()).isEqualTo(lineNumber);
  }
}