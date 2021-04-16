package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import com.google.common.net.MediaType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.taxiregister.dto.RegisterJobStatusDto;
import uk.gov.caz.taxiregister.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.repository.VehicleComplianceCheckerRepository;

@Slf4j
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/licensing-authority-data.sql",
    "classpath:data/sql/register-job-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public abstract class RegisterLicencesAbstractTest {

  static final UUID FIRST_UPLOADER_ID = UUID.fromString("6314d1d6-706a-40ce-b392-a0e618ab45b8");
  static final UUID SECOND_UPLOADER_ID = UUID.fromString("07447271-df3d-4217-9092-41f1252864b8");
  private static final String LA_1 = "la-1";
  private static final String LA_2 = "la-2";
  private ClientAndServer mockServer;

  @BeforeEach
  public void init() {
    RestAssured.port = getServerPort();
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/scheme-management";
    mockServer = ClientAndServer.startClientAndServer(1080);
    anyPostToVccsToPurgeCacheGives200OKResponse();
  }

  @AfterEach
  public void cleanup() {
    mockServer.stop();
  }

  private int currentExpectedTotalLicencesCount;

  private int currentExpectedTotalJobInfoCount;

  @Autowired
  private TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  @Autowired
  private LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void registerTest() {
    atTheBeginningThereShouldBeNoVehicles();

    whenLicencesAreRegisteredFromFirstLicensingAuthorityAgainstEmptyDatabase();
    thenAllShouldBeInsertedByFirstLicensingAuthority();
    andRegisterJobShouldHaveFinishedSuccessfully();
    andJobsInfoTableShouldContainNewRecordWithIdsOf(LA_1);

    whenLicencesAreRegisteredFromSecondLicensingAuthorityWithNoDataFromTheFirstOne();
    thenAllShouldBeInsertedBySecondLicensingAuthority();
    andLicencesFromFirstLicensingAuthorityShouldNotBeDeleted();
    andRegisterJobShouldHaveFinishedSuccessfully();
    andJobsInfoTableShouldContainNewRecordWithIdsOf(LA_2);
    andTwoCallsShouldBeMadeToVccsToPurgeCacheForAffectedVrms();

    whenThreeLicencesAreUpdatedByFirstLicensingAuthority();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedSuccessfully();
    andJobsInfoTableShouldContainNewRecordWithIdsOf(LA_1);

    whenThereAreFiveLicencesLessRegisteredByFirstAndSecondLicensingAuthority();
    thenFiveLicencesShouldBeDeletedForEveryLicensingAuthority();
    andRegisterJobShouldHaveFinishedSuccessfully();
    andJobsInfoTableShouldContainNewRecordWithIdsOf(LA_1, LA_2);

    whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateFormat();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(2);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateOrder();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicenceWithInvalidVrm();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicenceWithVrmStartingWithZero();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesWithManyErrors();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andRegisterJobShouldHaveFinishedWithOneErrorWithMessage(
        "CSV file upload unsuccessful. You will receive an email with error messages detailed.");
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesWithEmptyTaxiOrPhv();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesWithStartDateTooFarInPast();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesWithEndDateTooFarInFuture();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(1);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesWithInvalidWheelchairAccessibleValues();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithValidationErrorsCount(4);
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToRegisterLicencesByUnauthorisedLicensingAuthority();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithOneErrorWithMessage(
        "You are not authorised to submit data for la-4");
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereIsAttemptToUpdateLicensingAuthorityLockedByAnotherJob();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithOneErrorWithMessage(
        "Licence Authority is locked because it is being updated now by another Uploader");
    andJobsInfoTableShouldNotContainAnyNewRecords();

    whenThereAreDuplicatedLicensesInSingleCsv();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedWithOneErrorWithMessage(
        "There are multiple vehicles with the same VRN");
    andJobsInfoTableShouldNotContainAnyNewRecords();

    andAllFailedFilesShouldBeRemovedFromS3();
  }

  abstract int getServerPort();

  abstract void whenThereAreDuplicatedLicensesInSingleCsv();

  abstract void whenThreeLicencesAreUpdatedByFirstLicensingAuthority();

  abstract void whenThereAreFiveLicencesLessRegisteredByFirstAndSecondLicensingAuthority();

  abstract void whenLicencesAreRegisteredFromFirstLicensingAuthorityAgainstEmptyDatabase();

  abstract void whenLicencesAreRegisteredFromSecondLicensingAuthorityWithNoDataFromTheFirstOne();

  abstract void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateFormat();

  abstract void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateOrder();

  abstract void whenThereIsAttemptToRegisterLicenceWithInvalidVrm();

  abstract void whenThereIsAttemptToRegisterLicenceWithVrmStartingWithZero();

  abstract void whenThereIsAttemptToRegisterLicencesWithManyErrors();

  abstract void whenThereIsAttemptToRegisterLicencesByUnauthorisedLicensingAuthority();

  abstract int getSecondLicensingAuthorityTotalLicencesCount();

  abstract void whenThereIsAttemptToRegisterLicencesWithEmptyTaxiOrPhv();

  abstract void whenThereIsAttemptToUpdateLicensingAuthorityLockedByAnotherJob();

  abstract void whenThereIsAttemptToRegisterLicencesWithStartDateTooFarInPast();

  abstract void whenThereIsAttemptToRegisterLicencesWithEndDateTooFarInFuture();

  abstract void whenThereIsAttemptToRegisterLicensesWithddMMyyyyFormat();

  abstract void whenThereIsAttemptToRegisterLicencesWithInvalidWheelchairAccessibleValues();

  abstract int getTotalVehiclesCountAfterFirstUpload();

  abstract Optional<String> getRegisterJobName();

  abstract void andAllFailedFilesShouldBeRemovedFromS3();

  private void thenTotalNumberOfRecordsStaysTheSame() {
    int count = taxiPhvLicencePostgresRepository.findAll().size();
    assertThat(count).isEqualTo(currentExpectedTotalLicencesCount);
  }

  private void andRegisterJobShouldHaveFinishedWithValidationErrorsCount(int errorsCount) {
    getRegisterJobName()
        .map(this::getJobInfo)
        .ifPresent(jobInfo -> {
          assertThat(jobInfo.getStatus()).isEqualByComparingTo(RegisterJobStatusDto.FAILURE);
          assertThat(jobInfo.getErrors()).hasSize(errorsCount);
        });
  }

  private void andRegisterJobShouldHaveFinishedWithOneErrorWithMessage(String errorMessage) {
    getRegisterJobName()
        .map(this::getJobInfo)
        .ifPresent(jobInfo -> {
          assertThat(jobInfo.getStatus()).isEqualByComparingTo(RegisterJobStatusDto.FAILURE);
          assertThat(jobInfo.getErrors()).hasSize(1);
          assertThat(jobInfo.getErrors()[0]).isEqualTo(errorMessage);
        });
  }

  private void andLicencesFromFirstLicensingAuthorityShouldNotBeDeleted() {
    assertThatTheNumberOfVehiclesIs(
        getTotalVehiclesCountAfterFirstUpload() + getSecondLicensingAuthorityTotalLicencesCount()
    );
  }

  private void thenAllShouldBeInsertedBySecondLicensingAuthority() {
    assertThatNumberOfLicencesBySecondLicensingAuthorityIs(
        getSecondLicensingAuthorityTotalLicencesCount()
    );
    currentExpectedTotalLicencesCount += getSecondLicensingAuthorityTotalLicencesCount();
  }

  private void thenFiveLicencesShouldBeDeletedForEveryLicensingAuthority() {
    assertThatTheNumberOfLicencesByLicensingAuthorityIs(getTotalVehiclesCountAfterFirstUpload() - 5,
        LA_1);
    assertThatTheNumberOfLicencesByLicensingAuthorityIs(
        getSecondLicensingAuthorityTotalLicencesCount() - 5, LA_2);
    currentExpectedTotalLicencesCount -= 2 * 5;
  }

  private void thenAllShouldBeInsertedByFirstLicensingAuthority() {
    assertThatTheNumberOfVehiclesIs(getTotalVehiclesCountAfterFirstUpload());
    currentExpectedTotalLicencesCount = getTotalVehiclesCountAfterFirstUpload();
  }

  private void andRegisterJobShouldHaveFinishedSuccessfully() {
    getRegisterJobName()
        .map(this::getJobInfo)
        .map(StatusOfRegisterCsvFromS3JobQueryResult::getStatus)
        .ifPresent(registerJobStatus ->
            assertThat(registerJobStatus).isEqualByComparingTo(RegisterJobStatusDto.SUCCESS)
        );
  }

  private void assertThatNumberOfLicencesBySecondLicensingAuthorityIs(int expectedVehiclesCount) {
    assertThatTheNumberOfLicencesByLicensingAuthorityIs(expectedVehiclesCount, LA_2);
  }

  private void assertThatTheNumberOfLicencesByLicensingAuthorityIs(int expectedVehiclesCount,
      String licensingAuthorityName) {
    long count = taxiPhvLicencePostgresRepository
        .findAll()
        .stream()
        .map(TaxiPhvVehicleLicence::getLicensingAuthority)
        .map(LicensingAuthority::getName)
        .filter(licensingAuthorityName::equals)
        .count();
    assertThat(count).isEqualTo(expectedVehiclesCount);
  }

  private void assertThatTheNumberOfVehiclesIs(int expectedVehiclesCount) {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = taxiPhvLicencePostgresRepository
        .findAll();
    assertThat(taxiPhvVehicleLicences).hasSize(expectedVehiclesCount);
  }

  private void atTheBeginningThereShouldBeNoVehicles() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = taxiPhvLicencePostgresRepository
        .findAll();

    assertThat(taxiPhvVehicleLicences).isEmpty();
  }

  private void andJobsInfoTableShouldContainNewRecordWithIdsOf(String... licensingAuthorities) {
    assertNewJobInfoRowHasBeenInserted();

    Integer[] expected = mapToIds(licensingAuthorities);
    Integer[] actual = jdbcTemplate.queryForObject(
        "SELECT LICENCE_AUTHORITY_ID FROM T_MD_REGISTER_JOBS_INFO ORDER BY INSERT_TIMESTMP DESC LIMIT 1",
        (resultSet, i) -> {
          Integer[] result = (Integer[]) resultSet.getArray(1).getArray();
          return result;
        });
    assertThat(actual).containsExactlyInAnyOrder(expected);
  }

  private void andJobsInfoTableShouldNotContainAnyNewRecords() {
    assertJobInfoRowsCountIsEqualTo(currentExpectedTotalJobInfoCount);
  }

  private void assertNewJobInfoRowHasBeenInserted() {
    currentExpectedTotalJobInfoCount += 1;
    assertJobInfoRowsCountIsEqualTo(currentExpectedTotalJobInfoCount);
  }

  private void assertJobInfoRowsCountIsEqualTo(int expected) {
    int jobsInfoRowsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_MD_REGISTER_JOBS_INFO");

    assertThat(jobsInfoRowsCount).isEqualTo(expected);
  }

  private Integer[] mapToIds(String[] licensingAuthorities) {
    Map<String, LicensingAuthority> all = licensingAuthorityPostgresRepository.findAll();
    return Arrays.stream(licensingAuthorities)
        .map(all::get)
        .map(LicensingAuthority::getId)
        .toArray(Integer[]::new);
  }

  final StatusOfRegisterCsvFromS3JobQueryResult getJobInfo(String jobName) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, correlationId)
        .when()
        .get("/register-csv-from-s3/jobs/{registerJobName}",
            jobName)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract().as(StatusOfRegisterCsvFromS3JobQueryResult.class);
  }

  private void anyPostToVccsToPurgeCacheGives200OKResponse() {
    mockServer.when(
        request()
            .withMethod("POST")
            .withPath(VehicleComplianceCheckerRepository.VCCS_CACHE_INVALIDATION_URL_PATH))
        .respond(
            response().withStatusCode(200)
        );
  }

  private void andTwoCallsShouldBeMadeToVccsToPurgeCacheForAffectedVrms() {
    mockServer
        .verify(postToVccsToPurgeVrms(
            "{\"vrms\":[\"LE35LMK\",\"ND84VSX\",\"LV98HYW\",\"RG35XNP\",\"DS98UDG\",\"DL76MWX\",\"BW91HUN\"]}"))
        .verify(postToVccsToPurgeVrms(
            "{\"vrms\":[\"SV57THC\",\"SW40NRN\",\"ZA14APJ\",\"ZC62OMB\",\"DS98UDG\"]}"));
  }

  private HttpRequest postToVccsToPurgeVrms(String body) {
    return request()
        .withMethod("POST")
        .withPath(VehicleComplianceCheckerRepository.VCCS_CACHE_INVALIDATION_URL_PATH)
        .withBody(json(body, MediaType.parse("application/json")));
  }
}
