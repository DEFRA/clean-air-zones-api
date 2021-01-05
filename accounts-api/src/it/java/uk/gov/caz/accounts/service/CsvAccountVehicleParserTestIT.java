package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import com.opencsv.CSVParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import uk.gov.caz.csv.exception.CsvInvalidFieldsCountException;

class CsvAccountVehicleParserTestIT {

  private CsvAccountVehicleParser csvAccountVehicleParser = new CsvAccountVehicleParser(
      new CSVParser());

  @Test
  public void shouldNotParseLineWithComma() throws IOException {
    // given
    String line = "ZC62OMB,";

    // when
    Throwable throwable = catchThrowable(() -> csvAccountVehicleParser.parseLineMulti(line));

    // then
    assertThat(throwable).isInstanceOf(CsvInvalidFieldsCountException.class);
  }

  @Test
  public void shouldParseLineWithDoubleQuote() throws IOException {
    // given
    String line = "\"ZC62OMB\"";

    // when
    String[] result = csvAccountVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB"});
  }

  @Test
  public void shouldParseLineWithAmpersandAndApostrophe() throws IOException {
    // given
    String line = "ZC62OMB & '";

    // when
    String[] result = csvAccountVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB & '"});
  }
}
