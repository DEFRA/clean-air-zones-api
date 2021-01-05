package uk.gov.caz.accounts.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
        assertThat(result).isEqualTo(input);
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
}