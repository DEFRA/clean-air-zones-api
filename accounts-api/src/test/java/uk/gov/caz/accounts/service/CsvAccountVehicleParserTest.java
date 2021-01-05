package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.csv.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.csv.exception.CsvMaxLineLengthExceededException;

@ExtendWith(MockitoExtension.class)
class CsvAccountVehicleParserTest {

  @Mock
  private ICSVParser delegate;

  @InjectMocks
  private CsvAccountVehicleParser parser;
  private static final String[] ANY_VALID_OUTPUT = new String[]{"1"};

  @ParameterizedTest
  @ValueSource(strings = {
      "comma ND84VSX",
      "underscore ND84V_SX",
      "space ND8 4VSX",
      "ampersand ND84VSX&",
      "apostrophe ND84V'SX'",
      "left parenthesis (ND84VSX",
      "right parenthesis )OND84VSX",
      "asterisk N*D84VSX",
      "dot ND84VS.X",
      "slash ND84V/SX",
      "percent sign %ND84VSX",
      "exclamation mark !ND84VSX",
      "plus sign ND84VSX+",
      "colon ND84V;SX",
      "equals sign N=D84VSX",
      "question mark ?ND84VSX",
      "at sign @ND84VSX",
      "left square bracket [ND84VSX",
      "right square bracket ]ND84VSX",
      "circumflex accent ^ND84VSX",
      "left curly bracket {ND84VSX",
      "right curly bracket }ND84VSX",
      "tilde ~ND84VSX",
  })
  public void shouldAcceptLinesWithAcceptedCharacters(String line) throws IOException {
    // given
    given(delegate.parseLineMulti(line)).willReturn(ANY_VALID_OUTPUT);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "pound ND8£4VSX",
      "dollar N$D84VSX",
      "hash N#D84VSX",
  })
  public void shouldRejectLinesWithUnacceptedCharacters(String line) {
    // given

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldRejectLinesWithUnacceptedCharactersInPostProcessing() throws IOException {
    // given
    String line = "a,bł,c";

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldRejectEmptyLine() {
    // given
    String line = "";

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldRejectTooLongLine() {
    // given
    String line = Strings.repeat("a", CsvAccountVehicleParser.MAX_LINE_LENGTH + 1);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvMaxLineLengthExceededException.class);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseLineMulti() throws IOException {
    // given
    String line = "ND84VSX";
    given(delegate.parseLineMulti(line)).willReturn(ANY_VALID_OUTPUT);

    // when
    String[] result = parser.parseLineMulti(line);

    // then
    then(result).containsExactly(ANY_VALID_OUTPUT);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetSeparator() {
    // given
    char separator = ',';
    given(delegate.getSeparator()).willReturn(separator);

    // when
    char result = parser.getSeparator();

    // then
    then(result).isEqualTo(separator);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetQuotechar() {
    // given
    char quoteChar = 'x';
    given(delegate.getQuotechar()).willReturn(quoteChar);

    // when
    char result = parser.getQuotechar();

    // then
    then(result).isEqualTo(quoteChar);
  }

  @Test
  public void shouldCallDelegateWhenCallingIsPending() {
    // given
    boolean isPending = true;
    given(delegate.isPending()).willReturn(isPending);

    // when
    boolean result = parser.isPending();

    // then
    then(result).isEqualTo(isPending);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseLine() throws IOException {
    // given
    String[] output = new String[]{"output"};
    given(delegate.parseLine(any())).willReturn(output);

    // when
    String[] result = parser.parseLine("anything");

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseToLine() {
    // given
    String output = "output";
    given(delegate.parseToLine(any(), anyBoolean())).willReturn(output);

    // when
    String result = parser.parseToLine(new String[0], false);

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingNullFieldIndicator() {
    // given
    CSVReaderNullFieldIndicator output = CSVReaderNullFieldIndicator.NEITHER;
    given(delegate.nullFieldIndicator()).willReturn(output);

    // when
    CSVReaderNullFieldIndicator result = parser.nullFieldIndicator();

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetPendingText() {
    // given
    String output = "output";
    given(delegate.getPendingText()).willReturn(output);

    // when
    String result = parser.getPendingText();

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingSetErrorLocale() {
    // given
    Locale locale = Locale.CANADA_FRENCH;

    // when
    parser.setErrorLocale(locale);

    // then
    verify(delegate).setErrorLocale(locale);
  }

  @Test
  public void shouldThrowCsvInvalidFieldsCountExceptionWhenFieldsCntIsTooHigh() throws IOException {
    // given
    String line = "any input";
    given(delegate.parseLineMulti(anyString()))
        .willReturn(new String[] {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0"});

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidFieldsCountException.class);
  }
}