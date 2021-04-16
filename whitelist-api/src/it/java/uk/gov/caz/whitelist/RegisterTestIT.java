package uk.gov.caz.whitelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;
import static uk.gov.caz.whitelist.controller.Constants.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.whitelist.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.whitelist.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.whitelist.dto.RegisterJobStatusDto;
import uk.gov.caz.whitelist.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.whitelist.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.whitelist.model.CategoryType;
import uk.gov.caz.whitelist.model.CsvContentType;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;
import uk.gov.caz.whitelist.repository.WhitelistedVehicleDtoCsvRepository;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor;

/**
 * This class provides storage-specific methods for inserting Vehicles.
 *
 * It uses (and thus tests) {@link uk.gov.caz.whitelist.service.RegisterFromCsvCommand} command.
 */
@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
public class RegisterTestIT {

  private static final UUID FIRST_UPLOADER_ID = UUID
      .fromString("6314d1d6-706a-40ce-b392-a0e618ab45b8");
  private static final UUID SECOND_UPLOADER_ID = UUID
      .fromString("07447271-df3d-4217-9092-41f1252864b8");
  private static final UUID STATE_TEST_1_UPLOADER_ID = UUID
      .fromString("07447271-df3d-4217-9092-41f125281111");
  private static final UUID STATE_TEST_2_UPLOADER_ID = UUID
      .fromString("07447271-df3d-4217-9092-41f125282222");
  private static final UUID ERRORS_UPLOADER = UUID
      .fromString("07447271-df3d-4217-9092-41f125283333");
  private static final String UPLOADER_EMAIL = "uploader@dummy.eu";
  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");
  private static final int FIRST_UPLOADER_TOTAL_VEHICLES_COUNT = 3;

  private static final String BUCKET_NAME = String.format(
      "whitelisted-vehicles-data-%d",
      System.currentTimeMillis()
  );

  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
      FIRST_UPLOADER_ID.toString(),
      new String[]{"first-uploader-records-all.csv"},
      SECOND_UPLOADER_ID.toString(),
      new String[]{"second-uploader-max-validation-errors-exceeded.csv", "duplicated-vehicles.csv",
      "second-uploader-mixed-parse-and-business-validation-errors.csv"},
      STATE_TEST_1_UPLOADER_ID.toString(),
      new String[]{"database-state-test-file-1.csv", "category-mapping-test-file.csv"},
      STATE_TEST_2_UPLOADER_ID.toString(),
      new String[]{"database-state-test-file-2.csv"},
      ERRORS_UPLOADER.toString(),
      new String[]{"category-errors.csv", "csv-max-line-length-exceeded.csv",
          "empty-rows-in-between.csv"}
  );

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${application.validation.max-errors-count}")
  private int maxErrorsCount;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;

  private volatile StatusOfRegisterCsvFromS3JobQueryResult queryResult;

  @BeforeEach
  public void setUp() {
    createBucketAndFilesInS3();
    setUpRestAssured();
  }

  @AfterEach
  public void tearDown() {
    deleteBucketAndFilesFromS3();
  }

  @Test
  public void registerTest() {
    atTheBeginningThereShouldBeNoVehicles();

    whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader();
    thenAllShouldBeInserted();
    andThereShouldBeNoErrors();

    whenVehiclesAreRegisteredBySecondUploader(
        "second-uploader-max-validation-errors-exceeded.csv");
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobContainsSpecificErrors();
    andJobShouldFinishWithFailureStatus();
    andThereShouldBeMaxValidationErrors();

    whenVehiclesAreRegisteredBySecondUploader(
        "second-uploader-mixed-parse-and-business-validation-errors.csv");
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobShouldFinishWithFailureStatus();
    andJobContainsMixedParseAndBusinessValidationErrors();

    whenMultipleVehiclesAreRegisteredWithTheSameVrnBySecondUploader();
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobShouldFinishWithFailureStatus();
    andAllFailedFilesShouldBeRemovedFromS3(SECOND_UPLOADER_ID);
  }

  private void whenMultipleVehiclesAreRegisteredWithTheSameVrnBySecondUploader() {
    registerVehiclesFrom("duplicated-vehicles.csv");
  }

  @Test
  public void shouldVerifyProperDatabaseState() {
    atTheBeginningThereShouldBeNoVehicles();

    whenFirstFileIsUploaded();
    thenProperWhiteListVehiclesShouldExists();
    andThenWhitelistVehicleZC00001ExistsAndHaveReason("reason 1");
    andThereShouldBeNoErrors();

    whenSecondFileUploaded("database-state-test-file-2.csv");
    andThereShouldBeNoErrors();
    thenProperWhiteListVehiclesShouldExistsAgain();
    andThenWhitelistVehicleZC00001ExistsAndHaveReason("reason 1 updated");
    andThenWhitelistVehicleHasCategory("ZC00001", "Early Adopter");
    andThenWhitelistVehicleHasCategory("ZC00002", "Non-UK Vehicle");
  }

  @Test
  public void shouldReturnSpecificErrors() {
    atTheBeginningThereShouldBeNoVehicles();

    whenVehiclesAreRegisteredByThirdUploader("category-errors.csv");
    thenNoVehiclesUploaded();
    andJobShouldFinishWithFailureStatus();
    andCategoryJobContainsSpecificErrors();

    whenVehiclesAreRegisteredByThirdUploader("csv-max-line-length-exceeded.csv");
    thenNoVehiclesUploaded();
    andJobShouldFinishWithFailureStatus();
    andErrorShouldBeMaxLineLengthExceededInCSV();

    whenVehiclesAreRegisteredByThirdUploader("empty-rows-in-between.csv");
    thenNoVehiclesUploaded();
    andJobShouldFinishWithFailureStatus();
    andErrorsShouldNotContainDuplicatedVrnsButOnlyLinesValidation();
    andAllFailedFilesShouldBeRemovedFromS3(ERRORS_UPLOADER);
  }

  @Test
  public void shouldHaveProperValuesInDbForSpecificCategory() {
    atTheBeginningThereShouldBeNoVehicles();

    whenVehiclesAreRegisteredByThirdUploader("category-mapping-test-file.csv");
    andThenThereShouldBeVehicleWithCorrectCategoryInDB("CA00001", CategoryType.EARLY_ADOPTER);
    andThenThereShouldBeVehicleWithCorrectCategoryInDB("CA00002", CategoryType.NON_UK_VEHICLE);
    andThenThereShouldBeVehicleWithCorrectCategoryInDB("CA00003", CategoryType.PROBLEMATIC_VRN);
    andThenThereShouldBeVehicleWithCorrectCategoryInDB("CA00004", CategoryType.EXEMPTION);
    andThenThereShouldBeVehicleWithCorrectCategoryInDB("CA00005", CategoryType.OTHER);
    andThereShouldBeNoErrors();
  }

  private void andThenThereShouldBeVehicleWithCorrectCategoryInDB(String vrn,
      CategoryType categoryType) {
    Optional<WhitelistVehicle> vehicleFromDb = whitelistVehiclePostgresRepository
        .findOneByVrn(vrn);
    assertThat(vehicleFromDb).isPresent();
    assertThat(vehicleFromDb.get().getCategory()).isEqualTo(categoryType.getCategory());
    assertThat(vehicleFromDb.get().isExempt()).isEqualTo(categoryType.isExempt());
    assertThat(vehicleFromDb.get().isCompliant()).isEqualTo(categoryType.isCompliant());
  }

  private void thenNoVehiclesUploaded() {
    assertThat(countAllVehicles()).isEqualTo(0);
  }

  private void atTheBeginningThereShouldBeNoVehicles() {
    assertThat(countAllVehicles()).isEqualTo(0);
  }

  private void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader() {
    registerVehiclesFrom("first-uploader-records-all.csv");
  }

  private void thenAllShouldBeInserted() {
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void andThereShouldBeNoErrors() {
    boolean hasErrors = queryResult.getErrors() == null || queryResult.getErrors().length == 0;
    assertThat(hasErrors).isTrue();
  }

  private void andJobContainsMixedParseAndBusinessValidationErrors() {
    String errorMessage = getErrorMessage();
    failedJobContainsError(errorMessage,
        "Invalid length of Manufacturer field (actual length: 51, max allowed length: 50).");
    failedJobContainsError(errorMessage,
        "Line 1: Line contains invalid number of fields (actual value: 1, allowable value: 5). Please make sure you have not included a header row.");
    failedJobContainsError(errorMessage,
        "Line 2: VRN should have from 2 to 14 characters instead of 24.");
    failedJobContainsError(errorMessage,
        "Line 3: Action field should be empty or contain one of: C, D, U");
    failedJobContainsError(errorMessage,
        "Line 4: Line contains invalid number of fields (actual value: 1, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 5: You can't delete that number plate as it doesn't exist in the database.");
  }

  private void andErrorShouldBeMaxLineLengthExceededInCSV() {
    assertThat(queryResult.getErrors())
        .isEqualTo(new String[]{
            "Line 1: Line is too long (actual value: 147, allowed value: 140). Please make sure you have not included a header row."});
  }

  private void whenVehiclesAreRegisteredBySecondUploader(String file) {
    registerVehiclesFrom(file);
  }

  private void thenNoVehiclesShouldBeRegisteredBySecondUploader() {
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void andJobContainsSpecificErrors() {
    String errorMessage = getErrorMessage();
    failedJobContainsError(errorMessage,
        "Line 1: Line contains invalid number of fields (actual value: 1, allowable value: 5)"
            + ". Please make sure you have not included a header row.");
    failedJobContainsError(errorMessage,
        "Line 2: VRN should have from 2 to 14 characters instead of 24.");
    failedJobContainsError(errorMessage,
        "Line 3: Invalid format of VRN.");
    failedJobContainsError(errorMessage,
        "Line 4: Line contains invalid number of fields (actual value: 1, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 5: Blank row. Please remove or add data.");
    failedJobContainsError(errorMessage,
        "Line 6: Line contains invalid number of fields (actual value: 1, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 7: Line contains invalid number of fields (actual value: 2, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 8: Line contains invalid number of fields (actual value: 3, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 9: Line contains invalid number of fields (actual value: 1, allowable value: 5).");
    failedJobContainsError(errorMessage,
        "Line 11: VRN should have from 2 to 14 characters instead of 64.");
  }

  private void failedJobContainsError(String errors, String error) {
    assertThat(errors).contains(error);
  }

  void andAllFailedFilesShouldBeRemovedFromS3(UUID uploader) {
    Arrays.stream(UPLOADER_TO_FILES.get(uploader.toString())).forEach(file -> {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(file)
          .build();

      Throwable throwable = catchThrowable(() -> s3Client.getObject(getObjectRequest));

      assertThat(throwable).isInstanceOf(NoSuchKeyException.class);
    });
  }

  private void andJobShouldFinishWithFailureStatus() {
    assertThat(queryResult.getStatus()).isEqualTo(RegisterJobStatusDto.FAILURE);
  }

  private void andThereShouldBeMaxValidationErrors() {
    assertThat(queryResult.getErrors()).isNotNull();
    assertThat(queryResult.getErrors()).hasSize(maxErrorsCount);
  }

  private void whenFirstFileIsUploaded() {
    registerVehiclesFrom("database-state-test-file-1.csv");
  }

  private void thenProperWhiteListVehiclesShouldExists() {
    vehiclesShouldExist(STATE_TEST_1_UPLOADER_ID, "ZC00001", "ZC00002", "ZC00003", "ZC00004",
        "ZC00005", "ZC00006", "ZC00007", "ZC00008");
  }

  private void andThenWhitelistVehicleZC00001ExistsAndHaveReason(String reason) {
    Optional<WhitelistVehicle> vehicleFromDb = whitelistVehiclePostgresRepository
        .findOneByVrn("ZC00001");
    assertThat(vehicleFromDb).isPresent();
    assertThat(vehicleFromDb.get().getReasonUpdated()).isEqualTo(reason);
  }

  private void andThenWhitelistVehicleHasCategory(String vrn, String category) {
    Optional<WhitelistVehicle> vehicleFromDb = whitelistVehiclePostgresRepository
        .findOneByVrn(vrn);
    assertThat(vehicleFromDb).isPresent();
    assertThat(vehicleFromDb.get().getCategory()).isEqualTo(category);
  }

  private void whenSecondFileUploaded(String s) {
    registerVehiclesFrom(s);
  }

  private void thenProperWhiteListVehiclesShouldExistsAgain() {
    vehiclesShouldExist(STATE_TEST_2_UPLOADER_ID, "ZC00001", "ZC00002", "ZC00003", "ZC00004",
        "ZC00011", "ZC00009");
    vehiclesShouldNotExist("ZC00005", "ZC00006");
  }

  private void vehiclesShouldExist(UUID uploaderId, String... vrns) {
    Stream.of(vrns).forEach(vrn -> {
      Optional<WhitelistVehicle> vehicleFromDb = whitelistVehiclePostgresRepository
          .findOneByVrn(vrn);
      assertThat(vehicleFromDb).isPresent();
      assertThat(vehicleFromDb.get().getUploaderId()).isEqualTo(uploaderId);
      assertThat(vehicleFromDb.get().getUploaderEmail()).isEqualTo(UPLOADER_EMAIL);
    });
  }

  private void vehiclesShouldNotExist(String... vrns) {
    Stream.of(vrns).forEach(vrn -> {
      Optional<WhitelistVehicle> vehicleFromDb = whitelistVehiclePostgresRepository
          .findOneByVrn(vrn);
      assertThat(vehicleFromDb).isNotPresent();
    });
  }

  private void allVehiclesInDatabaseAreInsertedByFirstUploader() {
    assertThat(countAllVehicles()).isEqualTo(FIRST_UPLOADER_TOTAL_VEHICLES_COUNT);
  }

  private void whenVehiclesAreRegisteredByThirdUploader(String filename) {
    registerVehiclesFrom(filename);
  }

  private void andCategoryJobContainsSpecificErrors() {
    String errorMessage = getErrorMessage();
    failedJobContainsError(errorMessage,
        "\"vrn\":\"ZC00008\",\"title\":\"Value error\",\"detail\":"
            + "\"Line 1: Category field should contain one of: Early Adopter, Exemption, "
            + "Problematic VRN, Non-UK Vehicle, Other");
    failedJobContainsError(errorMessage,
        "\"vrn\":\"ZC00005\",\"title\":\"Value error\",\"detail\":"
            + "\"Line 3: Category field should contain one of: Early Adopter, Exemption, "
            + "Problematic VRN, Non-UK Vehicle, Other\"");
    failedJobContainsError(errorMessage,
        "\"title\":\"Value error\",\"detail\":\"Line 2: "
            + "Line contains invalid number of fields (actual value: 4, allowable value: 5).\"");
  }

  private void andErrorsShouldNotContainDuplicatedVrnsButOnlyLinesValidation() {
    assertThat(queryResult.getErrors())
        .isEqualTo(new String[]{
            "Line 5: Blank row. Please remove or add data.",
            "Line 6: Blank row. Please remove or add data.",
            "Line 7: Blank row. Please remove or add data.",
            "Line 8: Blank row. Please remove or add data.",
            "Line 9: Blank row. Please remove or add data.",
            "Line 10: Blank row. Please remove or add data.",
            "Line 11: Blank row. Please remove or add data.",
            "Line 12: Blank row. Please remove or add data.",
            "Line 13: Blank row. Please remove or add data.",
            "Line 14: Blank row. Please remove or add data."
        });
  }

  private void registerVehiclesFrom(String filename) {
    RegisterCsvFromS3JobHandle jobHandle = startJob(filename);
    Awaitility.with()
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to finish")
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> jobHasFinished(jobHandle));
  }

  private RegisterCsvFromS3JobHandle startJob(String filename) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(preparePayload(filename))
        .when()
        .post("/register-csv-from-s3/jobs")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .header(CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .extract().as(RegisterCsvFromS3JobHandle.class);
  }

  private boolean jobHasFinished(RegisterCsvFromS3JobHandle jobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryResult = getJobInfo(jobHandle.getJobName());
    this.queryResult = queryResult;
    return queryResult.getStatus() != RegisterJobStatusDto.RUNNING;
  }

  final StatusOfRegisterCsvFromS3JobQueryResult getJobInfo(String jobName) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured.given()
        .accept(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, correlationId)
        .when()
        .get("/register-csv-from-s3/jobs/{registerJobName}",
            jobName)
        .then()
        .statusCode(HttpStatus.OK.value())
        .header(CORRELATION_ID_HEADER, correlationId)
        .extract().as(StatusOfRegisterCsvFromS3JobQueryResult.class);
  }

  @SneakyThrows
  private String preparePayload(String filename) {
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(BUCKET_NAME,
        filename);
    return objectMapper.writeValueAsString(cmd);
  }

  private void createBucketAndFilesInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    uploadFilesToS3(UPLOADER_TO_FILES);
  }

  private void deleteBucketAndFilesFromS3() {
    deleteFilesFromS3(filesToDelete());
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
      String uploaderId = uploaderToFiles.getKey();
      String[] files = uploaderToFiles.getValue();

      for (String filename : files) {
        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
                .key(filename)
                .metadata(
                    ImmutableMap.of(
                        WhitelistedVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY, uploaderId,
                        CsvFileOnS3MetadataExtractor.UPLOADER_EMAIL_METADATA_KEY, UPLOADER_EMAIL,
                        CsvFileOnS3MetadataExtractor.CSV_CONTENT_TYPE_METADATA_KEY,
                        CsvContentType.WHITELIST_LIST.toString()
                    )
                ),
            FILE_BASE_PATH.resolve(filename));
      }
    }
  }

  private void deleteFilesFromS3(List<String> filenames) {
    for (String filename : filenames) {
      s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(filename));
    }
  }

  private List<String> filesToDelete() {
    return UPLOADER_TO_FILES.values()
        .stream()
        .map(Arrays::asList)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private void setUpRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/whitelisting";
  }

  private int countAllVehicles() {
    return JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_whitelist_vehicles.t_whitelist_vehicles");
  }

  private String getErrorMessage() {
    String sql = "SELECT errors FROM caz_whitelist_vehicles.t_whitelist_job_register WHERE status='FINISHED_FAILURE_VALIDATION_ERRORS'"
        + "order by LAST_MODIFIED_TIMESTMP desc limit 1";
    return jdbcTemplate.queryForObject(sql, String.class);
  }
}
