package uk.gov.caz.taxiregister.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.util.testfixtures.TestFixturesImporter;

@RestController
@Profile("dev | st | integration-tests")
@RequiredArgsConstructor
class TestFixturesController implements TestFixturesControllerApiSpec {

  static final String LOAD_TEST_FIXTURES_PATH = "/v1/load-test-data";

  private final TestFixturesImporter testFixturesImporter;

  @Override
  public ResponseEntity<Void> loadTestFixtures() {
    testFixturesImporter.loadTestFixturesFromFile();
    return ResponseEntity.noContent().build();
  }
}
