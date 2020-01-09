package uk.gov.caz.taxiregister.model;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationErrorTest {
  private static final String ANY_DETAIL = "details";
  private static final String ANY_VRM = "vrm1";

  @ParameterizedTest
  @ValueSource(ints = {-100, -87, -1, 0})
  public void shouldNotAcceptNonPositiveLineNumbers(int lineNumber) {
    // when
    Throwable throwable = catchThrowable(() -> ValidationError.valueError(ANY_VRM, ANY_DETAIL, lineNumber));

    // then
    then(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 18, 761})
  public void shouldIncludeLineNumberInDetailForValueErrors(int lineNumber) {
    // when
    ValidationError error = ValidationError.valueError(ANY_VRM, ANY_DETAIL, lineNumber);

    // then
    then(error.getDetail()).startsWith("Line " + lineNumber);
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 5, 912, 9012})
  public void shouldIncludeLineNumberInDetailForMissingFieldError(int lineNumber) {
    // when
    ValidationError error = ValidationError.missingFieldError(ANY_VRM, ANY_DETAIL, lineNumber);

    // then
    then(error.getDetail()).startsWith("Line " + lineNumber);
  }

  @Nested
  class Comparable {
    @Test
    public void shouldSortByLineNumberWhenTwoObjectsHaveIt() {
      // given
      ValidationError withLowerLineNo = ValidationError.missingFieldError(ANY_VRM, "first", 1);
      ValidationError withGreaterLineNo = ValidationError.missingFieldError(ANY_VRM, "second", 2);

      // when
      int compareGreaterWithLowerResult = withGreaterLineNo.compareTo(withLowerLineNo);
      int compareLowerWithGreaterResult = withLowerLineNo.compareTo(withGreaterLineNo);
      int compareSameResult = withLowerLineNo.compareTo(withLowerLineNo);

      // then
      then(compareGreaterWithLowerResult).isPositive();
      then(compareLowerWithGreaterResult).isNegative();
      then(compareSameResult).isZero();
    }

    @Test
    public void shouldOrderByRecordsContainingLineNumbersAndThenWithoutThem() {
      // given
      ValidationError withoutLineNo = ValidationError.missingFieldError(ANY_VRM, "first");
      ValidationError withLineNo = ValidationError.missingFieldError(ANY_VRM, "second", 15);

      // when
      int compareLineNumberWithNoLineNumberResult = withLineNo.compareTo(withoutLineNo);
      int compareNoLineNumberWithLineNumberResult = withoutLineNo.compareTo(withLineNo);

      // then
      then(compareLineNumberWithNoLineNumberResult).isNegative();
      then(compareNoLineNumberWithLineNumberResult).isPositive();
    }
  }
}