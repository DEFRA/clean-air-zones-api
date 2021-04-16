package uk.gov.caz.whitelist.util.testfixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.caz.whitelist.dto.TestFixturesVehicleDto;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.service.WhitelistedVehicleDtoToModelConverter;

/**
 * Parses a file containing whitelist vehicles in JSON format.
 */
@Component
@Profile("dev | st | integration-tests")
@Slf4j
class TestFixturesParser {

  private final ObjectMapper objectMapper;
  private final String testFixturesLocation;
  private final WhitelistedVehicleDtoToModelConverter vehiclesConverter;

  TestFixturesParser(ObjectMapper objectMapper,
      WhitelistedVehicleDtoToModelConverter vehiclesConverter,
      @Value("${application.test-fixtures-location}") String fixturesLocation) {
    this.objectMapper = objectMapper;
    this.vehiclesConverter = vehiclesConverter;
    this.testFixturesLocation = fixturesLocation;
  }

  /**
   * Parses a file located at {@code application.test-fixtures-location} containing whitelist
   * vehicles in JSON format and converts it to a {@link ConversionResults}.
   */
  @SneakyThrows
  public ConversionResults parseTestFixtures(UUID uploaderId, String uploaderEmail) {
    File fixturesLocation = new ClassPathResource(testFixturesLocation).getFile();
    return convertResults(parseJson(fixturesLocation), uploaderId, uploaderEmail);
  }

  private ConversionResults convertResults(List<TestFixturesVehicleDto> testFixtures,
      UUID uploaderId, String uploaderEmail) {

    List<WhitelistedVehicleDto> whitelistedVehicles = toWhitelistedVehicles(testFixtures);

    ConversionResults conversionResults = vehiclesConverter
        .convert(whitelistedVehicles, uploaderId, uploaderEmail);

    if (conversionResults.hasValidationErrors()) {
      List<ValidationError> errors = conversionResults.getValidationErrors();
      log.warn("There are {} validation errors in {}: {}", errors.size(),
          testFixturesLocation, errors);
      throw new IllegalArgumentException("Input validation fails");
    }

    return conversionResults;
  }

  private List<WhitelistedVehicleDto> toWhitelistedVehicles(
      List<TestFixturesVehicleDto> testFixtures) {
    return testFixtures
        .stream()
        .map(WhitelistedVehicleDto::from)
        .collect(Collectors.toList());
  }

  @SneakyThrows
  private List<TestFixturesVehicleDto> parseJson(File fixturesLocation) {
    return objectMapper.readValue(
        fixturesLocation,
        new TypeReference<List<TestFixturesVehicleDto>>() {
        }
    );
  }
}