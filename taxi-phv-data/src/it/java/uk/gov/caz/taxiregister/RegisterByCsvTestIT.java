package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.taxiregister.dto.RegisterJobStatusDto;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;

/**
 * This class provides storage-specific methods for inserting Vehicles. Please check {@link
 * RegisterLicencesAbstractTest} to get better understanding of tests steps that will be executed.
 *
 * It uses(and thus tests) {@link uk.gov.caz.taxiregister.service.RegisterFromCsvCommand} command.
 */
@FullyRunningServerIntegrationTest
@Slf4j
public class RegisterByCsvTestIT extends RegisterLicencesAbstractTest {

  private static final int FIRST_LICENSING_AUTHORITY_TOTAL_VEHICLES_COUNT = 7;
  private static final int SECOND_LICENSING_AUTHORITY_TOTAL_VEHICLES_COUNT = 6;

  private static final String BUCKET_NAME = String.format(
      "ntr-data-%d",
      System.currentTimeMillis()
  );

  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
      FIRST_UPLOADER_ID.toString(), new String[]{
          "la-1-all.csv",
          "la-1-all-and-three-modified.csv",
          "la-1-la-2-all-but-five-less-for-each-la.csv",
          "la-1-la-2-la-3-invalid-taxi-or-phv.csv",
          "la-1-invalid-licence-date-format.csv",
          "la-1-invalid-licence-dates-ordering.csv",
          "la-1-start-date-too-early.csv",
          "la-1-end-date-too-late.csv",
          "leeds-locked-licensing-authorities.csv"},
      SECOND_UPLOADER_ID.toString(), new String[]{
          "la-2-all.csv",
          "la-2-invalid-vrm.csv",
          "all-licensing-authorities.csv",
          "la-3-max-validation-errors-exceeded.csv"}
  );

  private static List<String> FILES_THAT_SHOULD_FAIL = Lists.newArrayList(
      "la-1-invalid-licence-date-format.csv",
      "la-1-invalid-licence-dates-ordering.csv",
      "la-1-start-date-too-early.csv",
      "la-1-end-date-too-late.csv",
      "la-2-invalid-vrm.csv",
      "la-3-max-validation-errors-exceeded.csv"
  );

  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private S3Client s3Client;

  @LocalServerPort
  int randomServerPort;

  private String jobName;

  private String[] errors;

  @BeforeEach
  private void prepareDataInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    uploadFilesToS3(UPLOADER_TO_FILES);
  }

  @AfterEach
  private void clearS3() {
    deleteFilesFromS3(filesToDelete());
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  @Override
  void whenLicencesAreRegisteredFromFirstLicensingAuthorityAgainstEmptyDatabase() {
    registerVehiclesFrom("la-1-all.csv");
  }

  @Override
  void whenLicencesAreRegisteredFromSecondLicensingAuthorityWithNoDataFromTheFirstOne() {
    registerVehiclesFrom("la-2-all.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateFormat() {
    registerVehiclesFrom("la-1-invalid-licence-date-format.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateOrder() {
    registerVehiclesFrom("la-1-invalid-licence-dates-ordering.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidVrm() {
    registerVehiclesFrom("la-2-invalid-vrm.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithTooManyErrors() {
    registerVehiclesFrom("la-3-max-validation-errors-exceeded.csv");
  }

  @Override
  void whenThereAreFiveLicencesLessRegisteredByFirstAndSecondLicensingAuthority() {
    registerVehiclesFrom("la-1-la-2-all-but-five-less-for-each-la.csv");
  }

  @Override
  void whenThreeLicencesAreUpdatedByFirstLicensingAuthority() {
    registerVehiclesFrom("la-1-all-and-three-modified.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesByUnauthorisedLicensingAuthority() {
    registerVehiclesFrom("all-licensing-authorities.csv");
  }

  @Override
  void whenThereIsAttemptToRegisterLicensesWithddMMyyyyFormat() {
    registerVehiclesFrom("la-1-dd-MM-yyyy-format.csv");
  }

  void whenThereIsAttemptToUpdateLicensingAuthorityLockedByAnotherJob() {
    registerVehiclesFrom("leeds-locked-licensing-authorities.csv");
  }

  @Override
  int getTotalVehiclesCountAfterFirstUpload() {
    return FIRST_LICENSING_AUTHORITY_TOTAL_VEHICLES_COUNT;
  }

  @Override
  int getSecondLicensingAuthorityTotalLicencesCount() {
    return SECOND_LICENSING_AUTHORITY_TOTAL_VEHICLES_COUNT;
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEmptyTaxiOrPhv() {
    registerVehiclesFrom("la-1-la-2-la-3-invalid-taxi-or-phv.csv");
    shouldReturnError("Line 4: Missing taxi/PHV value");
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithStartDateTooFarInPast() {
    registerVehiclesFrom("la-1-start-date-too-early.csv");
    shouldReturnError("Line 1: Start date cannot be more than 20 years in the past");
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEndDateTooFarInFuture() {
    registerVehiclesFrom("la-1-end-date-too-late.csv");
    shouldReturnError("Line 1: End date cannot be more than 20 years in the future");
  }

  @Override
  void andAllFailedFilesShouldBeRemovedFromS3() {
    FILES_THAT_SHOULD_FAIL.forEach(file -> {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(file)
          .build();

      Throwable throwable = catchThrowable(() -> s3Client.getObject(getObjectRequest));

      assertThat(throwable).isInstanceOf(NoSuchKeyException.class);

    });
  }

  private void registerVehiclesFrom(String filename) {
    RegisterCsvFromS3JobHandle jobHandle = startJob(filename);
    Awaitility.with()
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to finish")
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> jobHasFinished(jobHandle));

    this.jobName = jobHandle.getJobName();
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
        .extract().as(RegisterCsvFromS3JobHandle.class);
  }

  private boolean jobHasFinished(RegisterCsvFromS3JobHandle jobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryResult = getJobInfo(jobHandle.getJobName());
    errors = queryResult.getErrors();
    return queryResult.getStatus() != RegisterJobStatusDto.RUNNING;
  }

  @SneakyThrows
  private String preparePayload(String filename) {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(BUCKET_NAME, filename);
    return objectMapper.writeValueAsString(cmd);
  }

  @Override
  Optional<String> getRegisterJobName() {
    return Optional.ofNullable(jobName);
  }

  @Override
  int getServerPort() {
    return randomServerPort;
  }

  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
      String uploaderId = uploaderToFiles.getKey();
      String[] files = uploaderToFiles.getValue();

      for (String filename : files) {
        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
                .key(filename)
                .metadata(
                    Collections.singletonMap(
                        TaxiPhvLicenceCsvRepository.UPLOADER_ID_METADATA_KEY,
                        uploaderId
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

  private void shouldReturnError(String message) {
    assertThat(errors[0]).isEqualTo(message);
  }
}
