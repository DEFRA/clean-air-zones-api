package uk.gov.caz.taxiregister.reporting;

import static com.google.common.io.Files.asCharSource;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.taxiregister.tasks.ActiveLicencesInReportingWindowStarter;
import uk.gov.caz.taxiregister.util.AuditLogShaper;
import uk.gov.caz.taxiregister.util.LicenceInAuditLog;

@SpringBootTest(args = "in-integration-test-initialization")
@ActiveProfiles("integration-tests")
@Sql(scripts = "classpath:data/sql/clear.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@TestPropertySource(properties = {
    "tasks.active-licences-in-reporting-window.enabled=true",
    "spring.main.web-application-type=NONE"})
public class ActiveLicensesInReportingWindowTestIT {

  private static final String OUTPUT_CSV_FILE = "report_results.csv";
  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");

  // Licensing Authorities
  private static final String BIRMINGHAM = "Birmingham";
  private static final String LEEDS = "Leeds";
  // Vehicles
  private static final String BMW_VRN = "BMW123";
  private static final String AUDI_VRN = "AUD123";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ActiveLicencesInReportingWindowStarter activeLicencesStarter;

  @BeforeEach
  public void setup() {
    removeOutputCsvFile();
  }

  @AfterEach
  public void cleanup() {
    removeOutputCsvFile();
  }

  @Test
  public void testReportGeneration() throws IOException {
    // BMW in Birmingham
    // --- INSERT active licence for BMW_VRN in Birmingham between 2019-06-01 and 2019-07-01
    LicenceInAuditLog bmwInBirmingham = whenOn("2019-06-01_154329")
        .licenceFor(BMW_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2019-06-01", "2019-07-01")
        .wasUploaded();

    // --- UPDATE during reporting window
    whenOn("2019-06-03_185133")
        .licence(bmwInBirmingham)
        .wasUpdated();

    // --- DELETE during reporting window
    whenOn("2019-06-08_154329")
        .licence(bmwInBirmingham)
        .wasDeleted();

    // AUDI in Leeds
    // --- INSERT - active licence for AUDI_VRN in Leeds between 2019-02-01 and 2020-12-30
    LicenceInAuditLog audiInLeeds = whenOn("2019-01-01_154329")
        .licenceFor(AUDI_VRN).in(LEEDS)
        .withStartAndEndDates("2019-02-01", "2020-12-30")
        .wasUploaded();

    // --- UPDATE 1 before reporting window
    whenOn("2019-03-03_185133")
        .licence(audiInLeeds)
        .wasUpdated();

    // --- UPDATE 2 before reporting window
    whenOn("2019-04-03_185133")
        .licence(audiInLeeds)
        .wasUpdated();

    // --- DELETE inside reporting window
    whenOn("2019-05-10_154329")
        .licence(audiInLeeds)
        .wasDeleted();

    // BMW in Leeds
    // --- INSERT active licence for BMW_VRN in Leeds between 2019-06-01 and 2019-07-01
    LicenceInAuditLog bmwInLeeds = whenOn("2019-02-01_120000")
        .licenceFor(BMW_VRN).in(LEEDS)
        .withStartAndEndDates("2019-06-01", "2019-07-01")
        .wasUploaded();

    // --- DELETE before reporting window
    whenOn("2019-04-08_155555")
        .licence(bmwInLeeds)
        .wasDeleted();

    // AUDI in Birmingham
    // --- INSERT - active licence for AUDI_VRN in Birmingham between 2020-10-01 and 2020-10-01
    LicenceInAuditLog audiInBirmingham = whenOn("2019-04-30_180000")
        .licenceFor(AUDI_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2020-10-01", "2020-10-01")
        .wasUploaded();

    // --- UPDATE 1 inside reporting window
    whenOn("2019-05-01_185133")
        .licence(audiInBirmingham)
        .wasUpdated();

    ApplicationArguments args = new DefaultApplicationArguments("2019-05-01", "2020-10-01",
        getOutputCsvFilePath().toAbsolutePath().toString());
    activeLicencesStarter.run(args);

    assertThat(wholeCsvFile(getOutputCsvFilePath()))
        .isEqualTo(wholeCsvFile(getExpectedResultsCsvFilePath()));
  }

  private String wholeCsvFile(Path filePath) throws IOException {
    return asCharSource(filePath.toFile(), Charsets.UTF_8).read();
  }

  private AuditLogShaper whenOn(String dateTime) {
    return new AuditLogShaper(jdbcTemplate, dateTime);
  }

  @SneakyThrows
  private void removeOutputCsvFile() {
    Files.deleteIfExists(getOutputCsvFilePath());
  }

  private Path getOutputCsvFilePath() {
    String tempDir = System.getProperty("java.io.tmpdir");
    return Paths.get(tempDir, OUTPUT_CSV_FILE);
  }

  private Path getExpectedResultsCsvFilePath() {
    return FILE_BASE_PATH.resolve("expected-active-licences-in-reporting-window.csv");
  }
}
