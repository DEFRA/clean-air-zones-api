package uk.gov.caz.vcc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.vcc.util.LiquibaseWrapper;
import uk.gov.caz.vcc.util.TestFixturesLoader;

/**
 * Rest Controller with endpoints for applying liquibase schema migrations. 
 *
 */
@RestController
@Slf4j
public class DbSchemaMigrationControllerApi
    implements DbSchemaMigrationControllerApiSpec {
  public static final String PATH = "/v1/migrate-db";
  public static final String FALSE = "false";

  @Autowired(required = false)
  TestFixturesLoader testFixturesLoader;

  private final LiquibaseWrapper liquibaseWrapper;

  // Note the below two variables are drawn from env vars on the lambda function
  // as strings.
  private final String useRemoteApi;
  private final String testFixtureRefreshEnabled;

  /**
   * Creates an instance of {@link DbSchemaMigrationControllerApi}.
   *
   * @param liquibaseWrapper An instance of {@link LiquibaseWrapper}.
   */
  public DbSchemaMigrationControllerApi(LiquibaseWrapper liquibaseWrapper,
      @Value("${services.remote-vehicle-data.use-remote-api}") String useRemoteApi,
      @Value("${application.test-fixture-refresh.enabled:true}") String testFixtureRefreshEnabled) {
    this.liquibaseWrapper = liquibaseWrapper;
    this.useRemoteApi = useRemoteApi;
    this.testFixtureRefreshEnabled = testFixtureRefreshEnabled;

  }

  @Override
  public ResponseEntity<Void> migrateDb() {
    try {
      liquibaseWrapper.update();

      /*
       * If the remote vehicle data API is not being used, test data fixtures
       * will be reloaded by default. If, however, an explicit opt-out is (by
       * way of environment variable) is present re-population of test fixtures
       * will be skipped.
       */
      if (useRemoteApi.equalsIgnoreCase(FALSE)
          && testFixtureRefreshEnabled.equalsIgnoreCase("true")) {
        log.info("Begin refreshing test data fixtures");
        testFixturesLoader.loadTestVehiclesIntoDb();
        log.info("End refreshing test data fixtures");
      } else if (useRemoteApi.equalsIgnoreCase(FALSE)
          && testFixtureRefreshEnabled.equalsIgnoreCase(FALSE)) {
        log.info(
            "Skipping refresh of test data due to APPLICATION_TEST_FIXTURE_REFRESH_ENABLED "
            + "environment variable being set to false.");
      } else {
        log.info(
            "Skipping refresh of test data as harness is not used in execution environment.");
      }

      return ResponseEntity.status(HttpStatus.OK).body(null);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
