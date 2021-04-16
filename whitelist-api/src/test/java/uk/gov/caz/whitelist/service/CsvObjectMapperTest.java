package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.BDDAssertions.then;
import static uk.gov.caz.whitelist.model.Actions.DELETE;
import static uk.gov.caz.whitelist.model.Actions.UPDATE;
import static uk.gov.caz.whitelist.model.CategoryType.EARLY_ADOPTER;
import static uk.gov.caz.whitelist.model.CategoryType.EXEMPTION;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.CsvParseResult;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.service.validation.CsvAwareValidationMessageModifier;

class CsvObjectMapperTest {

  private CsvObjectMapper csvObjectMapper = new CsvObjectMapper(
      new CsvAwareValidationMessageModifier());

  @Test
  public void shouldReadValidCsvData() throws IOException {
    // given
    String csvLine = "zc62omb,Early Adopter,reason-1,manu-1,D";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).containsOnly(
        WhitelistedVehicleDto.builder()
            .vrn("ZC62OMB")
            .category(EARLY_ADOPTER.getCategory())
            .exempt(EARLY_ADOPTER.isExempt())
            .compliant(EARLY_ADOPTER.isCompliant())
            .reason("reason-1")
            .manufacturer(Optional.of("manu-1"))
            .action(DELETE.getActionCharacter())
            .lineNumber(1)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithExtraValues() throws IOException {
    // given
    String csvLine = "ZC62OMB,Early Adopter,reason-1,manu-1,D,extraValue1";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).isEmpty();
  }

  @Test
  public void shouldRemoveAllWhitespacesFromVrn() throws IOException {
    // given
    String csvLine = "Z   C62   OMB,Early Adopter,reason-1,manu-1,D";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).isNotEmpty();
    then(result.getWhitelistedVehicles().get(0).getVrn()).isEqualTo("ZC62OMB");
  }

  @Test
  public void shouldAddErrorForDuplicatedVrns() throws IOException {
    // given
    String csvLine = "ZC62OMB,Other,reason-2,manu-2,D\n"
        + "ZC62OMB,Other,reason-1,manu-1,D";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getValidationErrors()).isNotEmpty();
    then(result.getValidationErrors().get(0)).isEqualTo(
        ValidationError.valueError("There are multiple entries with the same VRN")
    );
  }

  @Test
  public void shouldIgnoreTooLongLines() throws IOException {
    // given
    String csvLine = "ZC62OMB,reason-1,manu-1,D" + Strings.repeat("ab", 100);

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", " ", "\t"})
  public void shouldIgnoreLinesWithWhitespaces(String csvLine) throws IOException {
    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).isEmpty();
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldIgnoreLinesWithUnacceptedCharacters() throws IOException {
    // given
    // contains '$' and '#'
    String csvLine = "Z$C62OMB,Early Adopter,r#eason-2,manu-2,D\n"
        + "ZC62OMB,Early Adopter,reason-1,manu-1,D";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).containsExactly(
        WhitelistedVehicleDto.builder()
            .vrn("ZC62OMB")
            .category(EARLY_ADOPTER.getCategory())
            .exempt(EARLY_ADOPTER.isExempt())
            .compliant(EARLY_ADOPTER.isCompliant())
            .reason("reason-1")
            .manufacturer(Optional.of("manu-1"))
            .action(DELETE.getActionCharacter())
            .lineNumber(2)
            .build()
    );
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError("Line contains invalid character(s), is empty or has "
            + "trailing comma character. Please make sure you have not included a header row.", 1)
    );
  }

  @Test
  public void shouldTrimLinesWithSpacesBetweenValueAndComma() throws IOException {
    // given
    String csvLine = "ZC62OMB , Early Adopter ,reason-1 ,manu-1,D";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getWhitelistedVehicles()).containsOnly(
        WhitelistedVehicleDto.builder()
            .vrn("ZC62OMB")
            .category(EARLY_ADOPTER.getCategory())
            .exempt(EARLY_ADOPTER.isExempt())
            .compliant(EARLY_ADOPTER.isCompliant())
            .reason("reason-1")
            .manufacturer(Optional.of("manu-1"))
            .action(DELETE.getActionCharacter())
            .lineNumber(1)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithTooFewAttributes() throws IOException {
    // given
    String csvLine = "ZC62OMB,Exemption,reason-1,manu-1,D\n"
        + "DL76MWX,2019-04-11\n"
        + "BD76MWY\n"
        + "BD76MWY,Early Adopter,reason-2,manu-2,A";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    WhitelistedVehicleDto[] expected = Arrays.array(
        WhitelistedVehicleDto.builder()
            .vrn("ZC62OMB")
            .category(EXEMPTION.getCategory())
            .exempt(EXEMPTION.isExempt())
            .compliant(EXEMPTION.isCompliant())
            .reason("reason-1")
            .manufacturer(Optional.of("manu-1"))
            .action(DELETE.getActionCharacter())
            .lineNumber(1)
            .build(),
        WhitelistedVehicleDto.builder()
            .vrn("BD76MWY")
            .category(EARLY_ADOPTER.getCategory())
            .exempt(EARLY_ADOPTER.isExempt())
            .compliant(EARLY_ADOPTER.isCompliant())
            .reason("reason-2")
            .manufacturer(Optional.of("manu-2"))
            .action("A")
            .lineNumber(4)
            .build()
    );
    then(result.getWhitelistedVehicles()).containsOnly(expected);
    then(result.getValidationErrors()).allSatisfy(
        validationError -> then(validationError.getDetail())
            .contains("Line contains invalid number of fields")
    );
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }
}