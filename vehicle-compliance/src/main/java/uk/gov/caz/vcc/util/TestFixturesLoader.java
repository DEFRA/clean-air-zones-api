package uk.gov.caz.vcc.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.caz.vcc.domain.LocalVehicle;
import uk.gov.caz.vcc.repository.LocalVehicleDetailsRepository;

/**
 * Utility class for loading vehicle details into a local test harness.
 *
 */
@Component
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api", havingValue = "false",
    matchIfMissing = true)
@Slf4j
public class TestFixturesLoader {

  @Autowired
  LocalVehicleDetailsRepository vehicleRepository;

  /**
   * Saves test fixtures into the DB at application startup, if not using remote vehicle data API.
   * 
   * @throws IOException if file does not exist on the build path.
   */
  public void loadTestVehiclesIntoDb() throws IOException {
    final LocalVehicle[] testVehicles = loadTestFixturesFromFile();

    log.info("TestFixturesLoader: Purging test vehicle data");
    vehicleRepository.deleteAll();

    log.info("TestFixturesLoader: Loading in test vehicle data from fixtures");
    Arrays.stream(testVehicles).forEach(v -> vehicleRepository.save(v));

    log.info("TestFixturesLoader: Loading test vehicle data from fixtures successful");
  }

  /**
   * Read test fixtures from file into List of Vehicle objects.
   * 
   * @return Vehicle objects read from test fixture file.
   * @throws IOException if file does not exist on the build path.
   */
  private LocalVehicle[] loadTestFixturesFromFile() throws IOException {
    File vehicleDetailsJson = new ClassPathResource("/db/fixtures/vehicle-details.json").getFile();

    // FAIL_ON_UNKNOWN_PROPERTIES = false to store description & expected outcomes on each fixture.
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(vehicleDetailsJson, LocalVehicle[].class);
  }
}
