package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.opencsv.CSVParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvWhitelistedVehicleParserTestIT {

  private CsvWhitelistedVehicleParser csvWhitelistedVehicleParser = new CsvWhitelistedVehicleParser(
      new CSVParser());

  @Test
  public void shouldParseLineWithComma() throws IOException {
    // given
    String line = "ZC62OMB,Early Adopter,\"Field with a comma, comma\",model-2,2019-05-17";

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(
        new String[]{"ZC62OMB", "Early Adopter", "Field with a comma, comma", "model-2",
            "2019-05-17"});
  }

  @Test
  public void shouldParseLineWithEmptyValues() throws IOException {
    // given
    String line = "ZC00006,Early Adopter,reason 6,,";

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC00006", "Early Adopter", "reason 6", "", ""});
  }

  @Test
  public void shouldParseLineWithDoubleQuote() throws IOException {
    // given
    String line = "ZC62OMB,Early Adopter,\"\"Hooyah!\"\",model-2,2019-05-17";

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(
        new String[]{"ZC62OMB", "Early Adopter", "\"Hooyah!\"", "model-2", "2019-05-17"});
  }
  
  @ParameterizedTest
  @ValueSource(strings = {"Mercedes-benz.}{().{}[]-/", "Bristol (BLMC)", "BSA D14/4", "Thor Ace 30.1"})
  public void shouldParseLineWithSpecialCharacters(String manufacturer) throws IOException {
    // given
    String line = "IS01102,Exemption,Reason," + manufacturer + ",2019-05-17";

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(
        new String[]{"IS01102", "Exemption", "Reason", manufacturer, "2019-05-17"});
  }


  @Test
  public void shouldParseLineWithCommaAndDoubleQuote() throws IOException {
    // given
    String line = "\"\"\"\"\"\"," // two quotes: ""
        + "\",,\"," // two commas: ,,
        + "\",\"," // one comma: ,,
        + "\"\"\",\"\",\"\"\"," // three quotes separated by commas: ",","
        + "\"\"\",,\"\"\""; // quote, comma, comma, quote

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{
        "\"\"",
        ",,",
        ",",
        "\",\",\"",
        "\",,\""
    });
  }

  @Test
  public void shouldParseLineWithAmpersandAndApostrophe() throws IOException {
    // given
    String line = "ZC62OMB,Early Adopter,category-2,a & b'c & d,2019-05-17";

    // when
    String[] result = csvWhitelistedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(
        new String[]{"ZC62OMB", "Early Adopter", "category-2", "a & b'c & d", "2019-05-17"});
  }
}