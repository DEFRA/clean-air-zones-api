package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import com.opencsv.CSVParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;

class CsvLicenceParserTestIT {

  private CsvLicenceParser csvLicenceParser = new CsvLicenceParser(new CSVParser());

  @Test
  public void shouldParseLineWithComma() throws IOException {
    // given
    String line = "ZC62OMB,2019-04-15,2019-05-17,PHV,\"Field with a comma, comma\",beBCC,true";

    // when
    String[] result = csvLicenceParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "2019-04-15", "2019-05-17", "PHV",
        "Field with a comma, comma", "beBCC", "true"});
  }

  @Test
  public void shouldParseLineWithDoubleQuote() throws IOException {
    // given
    String line = "ZC62OMB,2019-04-15,2019-05-17,PHV,\"\"Hooyah!\"\",beBCC,true";

    // when
    String[] result = csvLicenceParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "2019-04-15", "2019-05-17", "PHV",
        "\"Hooyah!\"", "beBCC", "true"});
  }

  @Test
  public void shouldParseLineWithCommaAndDoubleQuote() throws IOException {
    // given
    String line = "\"\"\"\"," // one quote: "
        + "\"\"\"\"\"\"," // two quotes: ""
        + "\",,\"," // two commas: ,,
        + "\"\"\",\"\",\"\"\"," // three quotes separated by commas: ",","
        + "\",\"\",\"," // comma, quote, comma: ,",
        + "\"\"\",,\"\"\"" // quote, comma, comma, quote
        + ",";

    // when
    String[] result = csvLicenceParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{
        "\"",
        "\"\"",
        ",,",
        "\",\",\"",
        ",\",",
        "\",,\"",
        ""
    });
  }

  @Test
  public void shouldParseLineWithAmpersandAndApostrophe() throws IOException {
    // given
    String line = "ZC62OMB,2019-04-15,2019-05-17,PHV,a & b'c & d,beBCC,";

    // when
    String[] result = csvLicenceParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "2019-04-15", "2019-05-17", "PHV",
        "a & b'c & d", "beBCC", ""});
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true,",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true,    ",
      "                ,                   ,    ",
      ",    ",
  })
  public void shouldRejectLineContainingTrailingComma(String line) {
    // given

    // when
    Throwable throwable = catchThrowable(() -> csvLicenceParser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidFieldsCountException.class);
  }
}