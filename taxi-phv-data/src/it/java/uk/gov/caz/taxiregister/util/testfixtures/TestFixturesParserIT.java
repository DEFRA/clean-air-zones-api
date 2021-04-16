package uk.gov.caz.taxiregister.util.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.VehicleToLicenceConverter;

class TestFixturesParserIT {

  private static final String EXISTING_TEST_FIXTURES_LOCATION = "/data/json/test-fixtures/licences.json";
  private static final String MALFORMED_DATA_FIXTURES_LOCATION = "/data/json/test-fixtures/malformed-data.json";

  private TestFixturesParser testFixturesParser;

  @Test
  public void shouldThrowExceptionWhenParsingMalformedJsonFile() {
    // given
    testFixturesParser = new TestFixturesParser(new ObjectMapper(), new VehicleToLicenceConverter(),
        MALFORMED_DATA_FIXTURES_LOCATION);

    // when
    Throwable throwable = catchThrowable(() -> testFixturesParser.parseTestFixtures());

    // then
    assertThat(throwable).isInstanceOf(JsonMappingException.class);
  }

  @Test
  public void shouldThrowExceptionUponConversionFailure() {
    // given
    VehicleToLicenceConverter converter = Mockito.mock(VehicleToLicenceConverter.class);
    when(converter.convert(anyList())).thenReturn(failedConversionResults());
    testFixturesParser = new TestFixturesParser(new ObjectMapper(), converter,
        EXISTING_TEST_FIXTURES_LOCATION);

    // when
    Throwable throwable = catchThrowable(() -> testFixturesParser.parseTestFixtures());

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Input validation fails");
  }

  @Test
  public void shouldParsePresentJsonFile() {
    // given
    testFixturesParser = new TestFixturesParser(new ObjectMapper(), new VehicleToLicenceConverter(),
        EXISTING_TEST_FIXTURES_LOCATION);

    // when
    Set<TaxiPhvVehicleLicence> result = testFixturesParser.parseTestFixtures();

    // then
    assertThat(result).hasSize(2);
  }

  private ConversionResults failedConversionResults() {
    return ConversionResults.from(
        Collections.singletonList(
            ConversionResult.failure(
                Collections.singletonList(
                    ValidationError.valueError("vrm", "details")
                )
            )
        )
    );
  }
}