package uk.gov.caz.taxiregister.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.reporting.VrmsWithActiveLicencesForLicensingAuthorityOnSpecifiedDateTestIT.ActiveLicencesForLicensingAuthorityAssertion.thenWhenAskedIf;

import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
@Sql(scripts = "classpath:data/sql/clear.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class VrmsWithActiveLicencesForLicensingAuthorityOnSpecifiedDateTestIT {

  // Licensing Authorities
  private static final String BIRMINGHAM = "Birmingham";
  private static final String LEEDS = "Leeds";

  private static final String BMW_VRN = "BMW123";
  private static final String AUDI_VRN = "AUD123";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void init() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/licensing-authorities/{licensingAuthorityId}/vrm-audit";
  }

  @Test
  public void testGettingLicencesForLicensingAuthority() {
    thereAreLicensingAuthoritiesNamed(BIRMINGHAM, LEEDS);

    // base case - no data in audit log
    thenWhenAskedIf(BIRMINGHAM)
        .hadActiveLicensesOn("2019-05-15")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();
    thenWhenAskedIf(LEEDS)
        .hadActiveLicensesOn("2019-05-16")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // --- INSERT
    // one active licence for BMW_VRN in Birmingham between 2019-05-01 and 2019-07-01
    LicenceInAuditLog bmwInBirmingham = whenOn("2019-05-01_154329")
        .licenceFor(BMW_VRN).in(BIRMINGHAM)
        .withStartAndEndDates("2019-05-01", "2019-07-01")
        .wasUploaded();

    // a. query date for active licence, but different licensing authority
    thenWhenAskedIf(LEEDS)
        .hadActiveLicensesOn("2019-05-10")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // b.1 query for inactive licence (past), correct licensing authority
    thenWhenAskedIf(BIRMINGHAM)
        .hadActiveLicensesOn("2017-01-10")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // b.2 query for inactive licence (past), correct licensing authority
    thenWhenAskedIf(BIRMINGHAM)
        .hadActiveLicensesOn("2019-07-02")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // c. query for active licence, correct licensing authority
    thenWhenAskedIf(BIRMINGHAM)
        .hadActiveLicensesOn("2019-05-15")
        .thereShouldBe(BMW_VRN)
        .vrmsWithActiveLicencesReturned();

    // --- DELETE
    // week after the upload the licence was deleted
    whenOn("2019-05-08_154329")
        .licence(bmwInBirmingham)
        .wasDeleted();

    // a. active licence (to 2019-07-01) was removed
    thenWhenAskedIf(BIRMINGHAM)
        .hadActiveLicensesOn("2019-05-15")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // -- INSERT
    // insert new licence during the time when it's active
    LicenceInAuditLog audiInLeeds = whenOn("2019-06-10_154329")
        .licenceFor(AUDI_VRN).in(LEEDS)
        .withStartAndEndDates("2019-06-01", "2019-07-01")
        .wasUploaded();

    // a. action timestamp check - licence was active, but added later
    thenWhenAskedIf(LEEDS)
        .hadActiveLicensesOn("2019-06-09")
        .thereShouldBeNoVrmsWithActiveLicencesReturned();

    // b. action timestamp check - licence is active on the add day
    thenWhenAskedIf(LEEDS)
        .hadActiveLicensesOn("2019-06-10")
        .thereShouldBe(AUDI_VRN)
        .vrmsWithActiveLicencesReturned();
  }

  private void thereAreLicensingAuthoritiesNamed(String birmingham, String leeds) {
    // Data inserted by main Liquibase scripts
  }

  private AuditLogShaper whenOn(String dateTime) {
    return new AuditLogShaper(jdbcTemplate, dateTime);
  }

  static class ActiveLicencesForLicensingAuthorityAssertion {
    private static Map<String, Integer> LA_NAME_TO_ID = ImmutableMap.of(
        "Birmingham", 1,
        "Leeds", 2
    );

    private final int licensingAuthorityId;
    private String queryDate;
    private List<String> expectedVrms;

    private ActiveLicencesForLicensingAuthorityAssertion(int licensingAuthorityId) {
      this.licensingAuthorityId = licensingAuthorityId;
    }

    public static ActiveLicencesForLicensingAuthorityAssertion thenWhenAskedIf(
        String licensingAuthority) {
      return new ActiveLicencesForLicensingAuthorityAssertion(
          LA_NAME_TO_ID.get(licensingAuthority)
      );
    }

    public ActiveLicencesForLicensingAuthorityAssertion hadActiveLicensesOn(String date) {
      this.queryDate = date;
      return this;
    }

    public ActiveLicencesForLicensingAuthorityAssertion thereShouldBe(String... vrms) {
      this.expectedVrms = Arrays.asList(vrms);
      return this;
    }

    public void vrmsWithActiveLicencesReturned() {
      List<String> actualVrms = callReportEndpointValidateResponseAndGetVrms();

      assertThat(actualVrms).containsExactlyElementsOf(expectedVrms);
    }

    private List<String> callReportEndpointValidateResponseAndGetVrms() {
      String correlationId = UUID.randomUUID().toString();

      return RestAssured
          .given()
          .accept(ContentType.JSON)
          .header(CORRELATION_ID_HEADER, correlationId)
          .pathParam("licensingAuthorityId", licensingAuthorityId)
          .queryParam("date", queryDate)

          .when()
          .get()

          .then()
          .statusCode(HttpStatus.OK.value())
          .log().ifValidationFails()
          .header(CORRELATION_ID_HEADER, correlationId)
          .body("auditDate", is(queryDate))
          .body("licensingAuthorityId", is(licensingAuthorityId))
          .extract()
          .path("vrmsWithActiveLicences");
    }

    public void thereShouldBeNoVrmsWithActiveLicencesReturned() {
      this.expectedVrms = Collections.emptyList();
      vrmsWithActiveLicencesReturned();
    }
  }
}
