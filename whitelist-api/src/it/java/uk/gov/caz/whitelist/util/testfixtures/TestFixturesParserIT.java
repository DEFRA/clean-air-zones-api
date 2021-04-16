package uk.gov.caz.whitelist.util.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.whitelist.annotation.IntegrationTest;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.service.WhitelistedVehicleDtoToModelConverter;

@IntegrationTest
class TestFixturesParserIT {

  private static final UUID TEST_FIXTURES_CREATE_UPLOADER_ID = UUID
      .fromString("db57cead-00ab-40d1-a297-9cb6a5f56f0f");
  private static final String TEST_FIXTURES_CREATE_UPLOADER_EMAIL = "testfixturescreate@gov.uk";
  private static final String EXISTING_TEST_FIXTURES_LOCATION = "/data/json/test-fixtures-vehicles.json";
  private static final String MALFORMED_DATA_FIXTURES_LOCATION = "/data/json/malformed-data.json";

  @Autowired
  private WhitelistedVehicleDtoToModelConverter vehiclesConverter;

  @Test
  public void shouldThrowExceptionWhenParsingMalformedJsonFile() {
    // given
    TestFixturesParser testFixturesParser = new TestFixturesParser(new ObjectMapper(),
        vehiclesConverter, MALFORMED_DATA_FIXTURES_LOCATION);
    // when
    Throwable throwable = catchThrowable(
        () -> testFixturesParser.parseTestFixtures(TEST_FIXTURES_CREATE_UPLOADER_ID,
            TEST_FIXTURES_CREATE_UPLOADER_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(JsonMappingException.class);
  }

  @Test
  public void shouldParsePresentJsonFile() {
    // given
    TestFixturesParser testFixturesParser = new TestFixturesParser(new ObjectMapper(),
        vehiclesConverter,
        EXISTING_TEST_FIXTURES_LOCATION);

    // when
    ConversionResults results = testFixturesParser
        .parseTestFixtures(TEST_FIXTURES_CREATE_UPLOADER_ID, TEST_FIXTURES_CREATE_UPLOADER_EMAIL);

    // then
    assertThat(results.getWhitelistVehiclesToSaveOrUpdate()).hasSize(4);
  }
}
