package uk.gov.caz.whitelist.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.whitelist.util.testfixtures.TestFixturesLoader;

@RestController
@Profile("dev | st | integration-tests")
@RequiredArgsConstructor
class TestFixturesController implements TestFixturesControllerApiSpec {

  static final String LOAD_TEST_FIXTURES_PATH = "/v1/load-test-data";

  private final TestFixturesLoader testFixturesLoader;

  @Override
  public ResponseEntity<Void> loadTestFixtures() {
    testFixturesLoader.loadTestFixtures();
    return ResponseEntity.noContent().build();
  }
}