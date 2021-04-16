package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.csv.CsvParseExceptionResolver;
import uk.gov.caz.csv.model.CsvParseResult;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

@IntegrationTest
class CsvTaxiPhvLicenceObjectMapperTestIT {

  @Autowired
  private ICSVParser parser;

  @Autowired
  private CsvParseExceptionResolver exceptionResolver;

  private CsvTaxiPhvLicenceObjectMapper csvObjectMapper;

  @BeforeEach
  public void setUp() {
    csvObjectMapper = new CsvTaxiPhvLicenceObjectMapper(parser, exceptionResolver);
  }

  @Nested
  class MaximumErrorCount {
    @Nested
    class WhenErrorsCountHasReachedMaxAllowedValue {

      @Nested
      class WhenFileHasBeenFullyParsed {
        @ParameterizedTest
        @ValueSource(strings = {
            // error, ok, error
            "ZC12OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
                + "ZC22OMB,2019-04-15,2019-05-17,PHV,abc,def,true\n"
                + "ZC32OMB,2019-04-15,2019-05-17,PHV,abc,def,true$",
            // error, error
            "ZC32OMB,2019-04-15,2019-05-17,PHV,abc,def,true$"
                + "\nZC52OMB,2019-04-15,2019-05-17,PHV,abc,def,true$"
        })
        public void shouldStopParsing(String input) throws IOException {
          // given
          csvObjectMapper = new CsvTaxiPhvLicenceObjectMapper(parser, exceptionResolver);

          // when
          CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(input));

          // then
          then(result.getValidationErrors()).hasSize(2);
        }
      }

      @Nested
      class WhenFileHasNotBeenFullyParsed {
        @ParameterizedTest
        @ValueSource(strings = {
            // error, ok, error, ok
            "ZC12OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
                + "ZC22OMB,2019-04-15,2019-05-17,PHV,abc,def,true\n"
                + "ZC32OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
                + "ZC42OMB,2019-04-15,2019-05-17,PHV,abc,def,true\r\n",
            // error, error, ok
            "ZC32OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
                + "ZC52OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
                + "ZC42OMB,2019-04-15,2019-05-17,PHV,abc,def,true$\n"
        })
        public void shouldNotIncludeTrailingRowInfoWhenFileHasNotBeenFullyParsed(String input) throws IOException {
          // given
          csvObjectMapper = new CsvTaxiPhvLicenceObjectMapper(parser, exceptionResolver);

          // when
          CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(input));

          // then
          then(result.getValidationErrors())
              .extracting((Extractor<CsvValidationError, String>) CsvValidationError::getDetail)
              .noneMatch(detail -> detail.endsWith("Empty row detected"));
        }
      }
    }
  }


  @Test
  public void shouldReadValidCsvData() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,true";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).containsOnly(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .description("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle("true")
            .lineNumber(1)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithExtraValues() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,false,extraValue1,extraValue2";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
  }

  @Test
  public void shouldIgnoreTooLongLines() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS" + Strings.repeat("ab", 100);

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {" \t", " ", "\t"})
  public void shouldIgnoreLinesWithWhitespaces(String csvLine) throws IOException {
    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).isEmpty();
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldIgnoreLinesWithUnacceptedCharacters() throws IOException {
    // given
    // contains '$' and '#'
    String csvLine = "line with error\n"
        + "$ZC62OMB,#2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,FALSE\n"
        + "ND84VSX,2019-04-14,2019-06-13,taxi,FBVoeKJGZF,Oretr,TrUe";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).containsExactly(
        VehicleDto.builder()
            .vrm("ND84VSX")
            .start("2019-04-14")
            .end("2019-06-13")
            .description("taxi")
            .licensingAuthorityName("FBVoeKJGZF")
            .licensePlateNumber("Oretr")
            .wheelchairAccessibleVehicle("TrUe")
            .lineNumber(3)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build()
    );
    then(result.getValidationErrors()).contains(
        CsvValidationError.with("Record doesn't match the data rule specifications", 1)
    );
  }

  @Test
  public void shouldNotIgnoreLineWithWrongBooleanFlag() throws IOException {
    // given
    String csvLine = "line with error\n"
        + "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,1";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).hasOnlyOneElementSatisfying(vehicleDto -> {
      assertThat(vehicleDto.getWheelchairAccessibleVehicle()).isEqualTo("1");
    });
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldStripAllWhitespacesFromVrn() throws IOException {
    // given
    String csvLine = "  ZC6     2 O M  B ,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).containsOnly(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .description("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle("")
            .lineNumber(1)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build()
    );
  }

  @Test
  public void shouldAcceptOptionalWheelchairValue() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getObjects()).containsOnly(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .description("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle("")
            .lineNumber(1)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithTooFewAttributes() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,false\n"
        + "DL76MWX,2019-04-11\n"
        + "ND84VSX,2019-04-14,2019-06-13,taxi,FBVoeKJGZF,Oretr,true";

    // when
    CsvParseResult<VehicleDto> result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    VehicleDto[] expected = Arrays.array(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .description("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle("false")
            .lineNumber(1)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build(),
        VehicleDto.builder()
            .vrm("ND84VSX")
            .start("2019-04-14")
            .end("2019-06-13")
            .description("taxi")
            .licensingAuthorityName("FBVoeKJGZF")
            .licensePlateNumber("Oretr")
            .wheelchairAccessibleVehicle("true")
            .lineNumber(3)
            .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
            .build()
    );
    then(result.getObjects()).containsOnly(expected);
    then(result.getValidationErrors()).hasOnlyOneElementSatisfying(
        validationError -> then(validationError.getDetail())
            .startsWith("Record doesn't match the data rule specifications")
    );
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }

}