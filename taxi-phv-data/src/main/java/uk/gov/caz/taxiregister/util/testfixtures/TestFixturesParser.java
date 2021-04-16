package uk.gov.caz.taxiregister.util.testfixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.VehicleToLicenceConverter;

/**
 * Parses a file containing licences in JSON format.
 */
@Component
@Profile("dev | st | integration-tests")
@Slf4j
class TestFixturesParser {

  private final ObjectMapper objectMapper;
  private final VehicleToLicenceConverter converter;
  private final String testFixturesLocation;

  TestFixturesParser(ObjectMapper objectMapper,
      VehicleToLicenceConverter converter,
      @Value("${application.test-fixtures-location}") String fixturesLocation) {
    this.objectMapper = objectMapper;
    this.converter = converter;
    this.testFixturesLocation = fixturesLocation;
  }

  /**
   * Parses a file located at {@code application.test-fixtures-location} containing licences in JSON
   * format and converts it to a collection of {@link TaxiPhvVehicleLicence}.
   */
  @SneakyThrows
  public Set<TaxiPhvVehicleLicence> parseTestFixtures() {
    File fixturesLocation = new ClassPathResource(testFixturesLocation).getFile();
    List<VehicleDto> vehicles = parseJson(fixturesLocation);
    ConversionResults conversionResults = toLicences(vehicles);
    return conversionResults.getLicences();
  }

  @SneakyThrows
  private List<VehicleDto> parseJson(File fixturesLocation) {
    return objectMapper.readValue(
        fixturesLocation,
        new TypeReference<List<VehicleDto>>() {
        }
    );
  }

  private ConversionResults toLicences(List<VehicleDto> vehicles) {
    ConversionResults conversionResults = converter.convert(vehicles);
    if (conversionResults.hasValidationErrors()) {
      List<ValidationError> errors = conversionResults.getValidationErrors();
      log.warn("There are {} validation errors in {}: {}", errors.size(),
          testFixturesLocation, errors);
      throw new IllegalArgumentException("Input validation fails");
    }
    return conversionResults;
  }
}
