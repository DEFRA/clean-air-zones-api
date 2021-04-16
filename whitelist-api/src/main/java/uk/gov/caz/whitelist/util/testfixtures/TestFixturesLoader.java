package uk.gov.caz.whitelist.util.testfixtures;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;
import uk.gov.caz.whitelist.service.RegisterService;
import uk.gov.caz.whitelist.service.WhitelistService;

@Component
@Profile("dev | st | integration-tests")
@RequiredArgsConstructor
@Slf4j
public class TestFixturesLoader {

  private static final UUID TEST_FIXTURES_CREATE_UPLOADER_ID = UUID
      .fromString("db57cead-00ab-40d1-a297-9cb6a5f56f0f");
  private static final String TEST_FIXTURES_CREATE_UPLOADER_EMAIL = "testfixturescreate@gov.uk";
  private static final UUID TEST_FIXTURES_DELETE_UPLOADER_ID = UUID
      .fromString("50b50c62-73dd-47bc-bd19-f90a7c0827f8");
  private static final String TEST_FIXTURES_DELETE_UPLOADER_EMAIL = "testfixturesdelete@gov.uk";

  private final TestFixturesParser testFixturesParser;
  private final RegisterService registerService;
  private final WhitelistService whitelistService;
  private final WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;

  /**
   * Deletes all licences from the database and imports the predefined ones from the JSON file.
   */
  @Transactional
  public void loadTestFixtures() {
    List<WhitelistVehicle> all = whitelistVehiclePostgresRepository.findAll();
    whitelistService.delete(all, TEST_FIXTURES_DELETE_UPLOADER_ID,
        TEST_FIXTURES_DELETE_UPLOADER_EMAIL);

    ConversionResults conversionResults = testFixturesParser
        .parseTestFixtures(TEST_FIXTURES_CREATE_UPLOADER_ID, TEST_FIXTURES_CREATE_UPLOADER_EMAIL);

    registerService.register(conversionResults, TEST_FIXTURES_CREATE_UPLOADER_ID,
            TEST_FIXTURES_CREATE_UPLOADER_EMAIL);
  }
}