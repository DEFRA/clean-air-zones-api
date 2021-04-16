package uk.gov.caz.taxiregister.util.testfixtures;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.service.RegisterService;

@Component
@Profile("dev | st | integration-tests")
@RequiredArgsConstructor
@Slf4j
public class TestFixturesImporter {

  private static final UUID TEST_FIXTURES_UPLOADER_ID = new UUID(0,0);

  private final TestFixturesParser testFixturesParser;
  private final RegisterService registerService;
  private final TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  /**
   * Deletes all licences from the database and imports the predefined ones from the
   * JSON file.
   */
  @Transactional
  public void loadTestFixturesFromFile() {
    Set<TaxiPhvVehicleLicence> licences = testFixturesParser.parseTestFixtures();

    taxiPhvLicencePostgresRepository.deleteAll();
    registerService.register(licences, TEST_FIXTURES_UPLOADER_ID);
  }
}
