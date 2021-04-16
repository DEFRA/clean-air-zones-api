package uk.gov.caz.taxiregister.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.taxiregister.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.taxiregister.util.AuditLogShaper;
import uk.gov.caz.taxiregister.util.LicenceInAuditLog;

@FullyRunningServerIntegrationTest
@Sql(scripts = {"classpath:data/sql/clear.sql", "classpath:data/sql/add-transient-la.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"classpath:data/sql/clear.sql", "classpath:data/sql/delete-transient-la.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class ActiveLicencesForVrnOnSpecifiedDateReportTestIT {

  // Licensing Authorities
  private static final String BIRMINGHAM = "Birmingham";
  private static final String LEEDS = "Leeds";
  private static final String TRANSIENT_LA = "Transient LA";
  private static final String UNKNOWN_LA = "UNKNOWN"; // used for LA absent in the database

  // Vehicles with VRN
  private static final String BMW_VRN = "BMW123";
  private static final String AUDI_VRN = "AUD123";
  private static final String SKODA_VRN = "SKD123";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void init() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/vehicles/{vrn}/licence-info-audit";
  }

  @Test
  public void testSimpleCasesOfReportToGetActiveLicencesForVrnOnSpecifiedDate() {
    thereAreLicensingAuthoritiesNamed(BIRMINGHAM, LEEDS, TRANSIENT_LA);
    andThereAreVehiclesWithVrn(BMW_VRN, AUDI_VRN, SKODA_VRN);

    // Change 1
    whenOn("2019-05-01_154329")
        .licenceFor(BMW_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2019-05-01", "2019-07-01")
        .wasUploaded();

    // Change 2
    whenOn("2019-05-01_154329")
        .licenceFor(BMW_VRN).in(LEEDS)
        .withStartAndEndDates("2019-05-01", "2019-07-01")
        .wasUploaded();

    // Change 3
    whenOn("2019-05-01_230812")
        .licenceFor(AUDI_VRN).in(LEEDS)
        .withStartAndEndDates("2019-03-01", "2019-12-30")
        .wasUploaded();

    // Today is later than active licence (to 2019-07-01) (Change 1)
    thenWhenAskedIf(BMW_VRN)
        .hasActiveLicensesToday()
        .thereShouldBeNoActiveLicences();

    // This is in range of Change 1 and Change 2
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-05-01")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM, LEEDS);

    // This is in range of Change 1 and Change 2
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-07-01")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM, LEEDS);

    // This is out of range of Change 1 and 2 (1 day before activation)
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-04-30")
        .thereShouldBeNoActiveLicences();

    // This is out of range of Change 1 and 2 (1 day after expiry date)
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-07-02")
        .thereShouldBeNoActiveLicences();

    // In range of Change 3
    thenWhenAskedIf(AUDI_VRN)
        .hadActiveLicensesOn("2019-05-01")
        .thereShouldBeActiveLicensesIn(LEEDS);

    // Audi has active licence since 2019-03-01 however it was added on 2019-05-01 (Change 3)
    thenWhenAskedIf(AUDI_VRN)
        .hadActiveLicensesOn("2019-04-01")
        .thereShouldBeNoActiveLicences();
  }

  @Test
  public void testMoreComplicatedCasesOfReportToGetActiveLicencesForVrnOnSpecifiedDate() {
    thereAreLicensingAuthoritiesNamed(BIRMINGHAM, LEEDS, TRANSIENT_LA);
    andThereAreVehiclesWithVrn(BMW_VRN, AUDI_VRN, SKODA_VRN);

    // Change 1
    whenOn("2019-01-01_154329")
        .licenceFor(BMW_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2019-01-01", "2019-01-31")
        .wasUploaded();

    // Change 2
    whenOn("2019-03-01_154329")
        .licenceFor(BMW_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2019-03-01", "2019-03-31")
        .wasUploaded();

    // Change 3
    LicenceInAuditLog bmwInLeeds = whenOn("2019-03-01_154329")
        .licenceFor(BMW_VRN).in(LEEDS)
        .withStartAndEndDates("2019-03-01", "2019-03-31")
        .wasUploaded();

    // Change 4 - remove entry from Change 3 but on 03.05
    whenOn("2019-03-05_154329")
        .licence(bmwInLeeds)
        .wasDeleted();

    // Change 5 - insert licence on 03.15
    whenOn("2019-03-15_154329")
        .licenceFor(BMW_VRN).in(LEEDS)
        .withStartAndEndDates("2019-03-15", "2019-03-31")
        .wasUploaded();

    // Change 6 - insert licence for a transient LA
    whenOn("2019-01-01_154329")
        .licenceFor(SKODA_VRN).in(TRANSIENT_LA)
        .withStartAndEndDates("2019-02-01", "2019-03-01")
        .wasUploaded();

    // Change 6 - remove transient LA (once it has expired)
    whenOn("2019-06-01_120000")
        .licenceAuthorityWasDeleted(TRANSIENT_LA);

    // Change 1 in effect
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-01-15")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM);

    // Change 1 expired
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-02-15")
        .thereShouldBeNoActiveLicences();

    // Change 2 and Change 3 in effect
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-03-02")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM, LEEDS);

    // Change 4 removed Change 3 = no Leeds
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-03-10")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM);

    // Change 2 and Change 5 in effect
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-03-20")
        .thereShouldBeActiveLicensesIn(BIRMINGHAM, LEEDS);

    // Change 6 - active licence, but LA was removed from the database
    thenWhenAskedIf(SKODA_VRN)
        .hadActiveLicensesOn("2019-02-03")
        .thereShouldBeActiveLicensesIn(UNKNOWN_LA);

    // All licences expired
    thenWhenAskedIf(BMW_VRN)
        .hadActiveLicensesOn("2019-04-10")
        .thereShouldBeNoActiveLicences();
  }

  private void thereAreLicensingAuthoritiesNamed(String birmingham, String leeds,
      String transientLa) {
    // Data inserted by main Liquibase scripts
  }

  private void andThereAreVehiclesWithVrn(String bmw1, String audi1, String skodaVrn) {
    // Dummy method to make test code more human readable.
    // Report relies only on data in audit.logged_actions table
    // so there is no need to put any licenses into DB.
  }

  private AuditLogShaper whenOn(String dateTime) {
    return new AuditLogShaper(jdbcTemplate, dateTime);
  }

  private ReportRunAssertion thenWhenAskedIf(String vrn) {
    return new ReportRunAssertion(vrn);
  }

  @RequiredArgsConstructor
  private static class ReportRunAssertion {

    private final String vrn;
    private String date;

    public ReportRunAssertion hadActiveLicensesOn(String date) {
      this.date = date;
      return this;
    }

    public ReportRunAssertion hasActiveLicensesToday() {
      this.date = null;
      return this;
    }

    public void thereShouldBeActiveLicensesIn(String... expectedLicensingAuthorities) {
      List<String> actualLicensingAuthorities = callReportEndpointValidateResponseAndGetNamesOfLicensingAuthorities();

      assertThat(actualLicensingAuthorities).hasSameSizeAs(expectedLicensingAuthorities);
      assertThat(actualLicensingAuthorities).contains(expectedLicensingAuthorities);
    }

    public void thereShouldBeNoActiveLicences() {
      List<String> licensingAuthorityNames = callReportEndpointValidateResponseAndGetNamesOfLicensingAuthorities();

      assertThat(licensingAuthorityNames).isEmpty();
    }

    private List<String> callReportEndpointValidateResponseAndGetNamesOfLicensingAuthorities() {
      String correlationId = UUID.randomUUID().toString();
      RequestSpecification requestSpecification = RestAssured
          .given()
          .contentType(ContentType.JSON)
          .accept(ContentType.JSON)
          .header(CORRELATION_ID_HEADER, correlationId)
          .pathParam("vrn", this.vrn);
      if (date != null) {
        requestSpecification.queryParam("date", date);
      }

      return requestSpecification
          .when()
          .get("")
          .then()
          .statusCode(HttpStatus.OK.value())
          .log().ifValidationFails()
          .header(CORRELATION_ID_HEADER, correlationId)
          .extract()
          .path("licensingAuthoritiesNames");
    }
  }
}
