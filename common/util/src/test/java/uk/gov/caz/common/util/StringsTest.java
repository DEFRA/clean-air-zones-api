package uk.gov.caz.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StringsTest {

  @Nested
  class Mask {

    @Nested
    class WhenNull {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String input = null;

        // when
        Throwable throwable = catchThrowable(() -> Strings.mask(input));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("'input' cannot be null");
      }
    }

    @Nested
    class WhenEmpty {

      @Test
      public void shouldReturnEmptyString() {
        // given
        String input = "";

        // when
        String result = Strings.mask(input);

        // then
        assertThat(result).isEmpty();
      }
    }

    @Nested
    class WhenShorterThanOrEqualToThreeCharacters {

      @ParameterizedTest
      @ValueSource(strings = {
          "a",
          "ab",
          "abc"
      })
      public void shouldReturnPassedInput(String input) {
        // given

        // when
        String result = Strings.mask(input);

        // then
        assertThat(result).hasSameSizeAs(input);
        assertThat(result).containsPattern(Pattern.compile("[*]+"));
      }
    }

    @Nested
    class WhenLongerThanThreeCharacters {

      @ParameterizedTest
      @ValueSource(strings = {
          "abcd",
          "aaaaaaaaaaaaaaaaa",
          "abcdefghijk"
      })
      public void shouldReturnPassedInput(String input) {
        // given

        // when
        String result = Strings.mask(input);

        // then
        assertThat(result).hasSameSizeAs(input);
        assertThat(result.substring(3)).contains("*");
        assertThat(result.substring(0, 3)).doesNotContain("***");
      }
    }
  }

  @Nested
  class IsValidUuid {

    @Nested
    class WhenNull {

      @Test
      public void shouldThrowNullPointerException() {
        // given
        String input = null;

        // when
        Throwable throwable = catchThrowable(() -> Strings.isValidUuid(input));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("'potentialUuid' cannot be null");
      }
    }

    @Nested
    class WhenInvalidUuid {

      @ParameterizedTest
      @ValueSource(strings = {"invalid-uuid", "4d4d8f3b-3b81-44f3-968d"})
      public void shouldReturnFalse(String input) {
        // given

        // when
        boolean result = Strings.isValidUuid(input);

        // then
        assertThat(result).isFalse();
      }
    }

    @Nested
    class WhenValidUuid {

      @ParameterizedTest
      @ValueSource(strings = {"584d18d6-566e-499e-9eff-6f30e375eccb",
          "1383bbd0-0869-47b5-8631-131c4c712375"})
      public void shouldReturnTrue(String input) {
        // given

        // when
        boolean result = Strings.isValidUuid(input);

        // then
        assertThat(result).isTrue();
      }
    }
  }

}