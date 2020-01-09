package uk.gov.caz.csv;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForwardingCsvParserTest {

  private static class CsvParser extends ForwardingCsvParser {

    CsvParser(ICSVParser delegate) {
      super(delegate);
    }

    @Override
    protected void afterParseLine(String[] result) {

    }

    @Override
    protected void beforeParseLine(String nextLine) {

    }
  }

  @Mock
  private ICSVParser delegate;

  private ForwardingCsvParser parser;

  @BeforeEach
  public void setUp() {
    parser = new CsvParser(delegate);
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedNullDelegate() {
    // given
    ICSVParser delegate = null;

    Throwable throwable = catchThrowable(() -> new CsvParser(delegate));

    then(throwable)
        .hasMessage("Delegate cannot be null")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseLineMulti() throws IOException {
    // given
    String line = "a,b";
    given(delegate.parseLineMulti(line)).willReturn(new String[]{"a", "b"});

    // when
    String[] result = parser.parseLineMulti(line);

    // then
    then(result).containsExactly("a", "b");
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
}