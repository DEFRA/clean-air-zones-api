package uk.gov.caz.accounts.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.csv.CsvParseExceptionResolver;
import uk.gov.caz.csv.model.CsvParseResult;

@IntegrationTest
class CsvAccountVehicleObjectMapperTestIT {

  @Autowired
  private CsvParseExceptionResolver exceptionResolver;

  @Autowired
  private CsvAccountVehicleObjectMapper csvAccountVehicleObjectMapper;

  @Test
  public void shouldReadEmptyFile() throws IOException {
    // given
    String csvLine = "";

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
    then(result.getValidationErrors()).isEmpty();
  }

  @Test
  public void shouldReadValidCsvData() throws IOException {
    // given
    String csvLine = "header\n"
        + "ZC62OMB";

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).containsOnly(
        AccountVehicleDto.builder()
            .vrn("ZC62OMB")
            .lineNumber(2)
            .build()
    );
  }

  @Test
  public void shouldRemoveAllWhitespacesFromVrn() throws IOException {
    // given
    String csvLine = "header\n"
        + "Z   C62   OMB";

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isNotEmpty();
    then(result.getObjects().get(0).getVrn()).isEqualTo("ZC62OMB");
  }

  @Test
  public void shouldIgnoreLinesWithExtraValues() throws IOException {
    // given
    String csvLine = "header\n"
        + "ZC62OMB,shabadaba";

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
  }

  @Test
  public void shouldIgnoreTooLongLines() throws IOException {
    // given
    String csvLine = "header\n"
        + "ZC62OMB" + Strings.repeat("ab", 20);

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"header\n\n", "header\n ", "header\n\t"})
  public void shouldIgnoreLinesWithWhitespaces(String csvLine) throws IOException {
    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldIgnoreLinesWithTooFewAttributes() throws IOException {
    // given
    String csvLine = "header\n"
        + "ZC62OMB\n"
        + "";

    // when
    CsvParseResult<AccountVehicleDto> result = csvAccountVehicleObjectMapper
        .read(toInputStream(csvLine));

    // then
    AccountVehicleDto[] expected = Arrays.array(
        AccountVehicleDto.builder()
            .vrn("ZC62OMB")
            .lineNumber(2)
            .build()
    );

    then(result.getObjects()).containsOnly(expected);
    then(result.getValidationErrors()).allSatisfy(
        validationError -> then(validationError.getDetail())
            .contains("Line contains invalid number of fields")
    );
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }
}
