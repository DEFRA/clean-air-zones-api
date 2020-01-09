package uk.gov.caz.csv;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvHeaderRowValidationMessageReplacerTest {

  private CsvHeaderRowValidationMessageReplacer messageModifier = new CsvHeaderRowValidationMessageReplacer();

  @ParameterizedTest
  @ValueSource(ints = {7, 8, 99})
  public void shouldNotIncludeInfoAboutHeaderForAllButFirstLine(int lineNumber) {
    // given
    String message = "Hello kitty";

    // when
    String output = messageModifier.replaceMessageWithHeaderRowErrorIfApplicable(message, lineNumber);

    // then
    then(output).doesNotEndWith(CsvHeaderRowValidationMessageReplacer.PRESENT_HEADER_MESSAGE_REPLACEMENT);
  }

  @Test
  public void shouldIncludeInfoAboutHeaderForFirstLine() {
    // given
    int lineNumber = 1;
    String message = "Hello kitty.";

    // when
    String output = messageModifier.replaceMessageWithHeaderRowErrorIfApplicable(message, lineNumber);

    // then
    then(output).isEqualTo(CsvHeaderRowValidationMessageReplacer.PRESENT_HEADER_MESSAGE_REPLACEMENT);
  }
}