package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import uk.gov.caz.taxiregister.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.taxiregister.controller.Constants;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;

/**
 * This class provides storage-specific methods for inserting Vehicles. Please check {@link
 * RegisterLicencesAbstractTest} to get better understanding of tests steps that will be executed.
 *
 * It uses(and thus tests) {@link uk.gov.caz.taxiregister.service.RegisterFromRestApiCommand}
 * command.
 */
@FullyRunningServerIntegrationTest
@Slf4j
public class RegisterByAPITestIT extends RegisterLicencesAbstractTest {

  private final String QUEUE_NAME = "job-clean-up-request";

  @Value("${aws.s3.payload-retention-bucket}")
  private String BUCKET_NAME;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private SqsClient sqsClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${application.job-clean-up-request.message-visibility-delay-in-seconds}")
  private int jobCleanupRequestDelayInSeconds;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  private void createSqsQueue() {
    CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(QUEUE_NAME)
        .build();
    CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);

    createBucket();
  }

  @AfterEach
  private void deleteSqsQueue() {
    GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(QUEUE_NAME)
        .build();
    String sqsQueueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder().queueUrl(sqsQueueUrl)
        .build();
    sqsClient.deleteQueue(deleteQueueRequest);

    deleteBucket();
  }

  @Override
  int getServerPort() {
    return randomServerPort;
  }

  @Override
  void whenThereAreDuplicatedLicensesInSingleCsv() {
    whenRegister("la-1-duplicated-licenses.json")
        .atTheBeginningBucketShouldBeEmpty()
        .thenRegisterByFirstUploader()
        .thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThreeLicencesAreUpdatedByFirstLicensingAuthority() {
    whenRegister("la-1-all-and-three-modified.json")
        .atTheBeginningBucketShouldBeEmpty()
        .thenRegisterByFirstUploader()
        .thenBucketShouldBeNotEmpty()
        .andContainsOneKey()
        .andJsonListWithSize(7);
  }

  @Override
  void whenLicencesAreRegisteredFromSecondLicensingAuthorityWithNoDataFromTheFirstOne() {
    whenRegister("la-2-all.json")
        .atTheBeginningBucketShouldBeEmpty()
        .thenRegisterBySecondUploader()
        .thenBucketShouldBeNotEmpty()
        .andContainsOneKey()
        .andJsonListWithSize(6);
  }

  @Override
  void whenThereAreFiveLicencesLessRegisteredByFirstAndSecondLicensingAuthority() {
    whenRegister("la-1-la-2-all-but-five-less-for-each-la.json")
        .atTheBeginningBucketShouldBeEmpty()
        .thenRegisterByFirstUploader()
        .thenBucketShouldBeNotEmpty()
        .andContainsOneKey()
        .andJsonListWithSize(3);
  }

  @Override
  void whenLicencesAreRegisteredFromFirstLicensingAuthorityAgainstEmptyDatabase() {
    whenRegister("la-1-all.json")
        .atTheBeginningBucketShouldBeEmpty()
        .thenRegisterByFirstUploader()
        .thenBucketShouldBeNotEmpty()
        .andContainsOneKey()
        .andJsonListWithSize(7);
  }

  void whenTooManyLicensesAreRegistered() {
    registerByFirstUploader("big-request.json").statusCode(HttpStatus.CREATED.value());
  }

  void whenFairAmountOfLicensesAreRegistered() {
    registerByFirstUploader("la-1-all.json").statusCode(HttpStatus.CREATED.value());
  }

  @Override
  int getTotalVehiclesCountAfterFirstUpload() {
    return 7;
  }

  @Override
  int getSecondLicensingAuthorityTotalLicencesCount() {
    return 6;
  }

  @Override
  Optional<String> getRegisterJobName() {
    return Optional.empty();
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateFormat() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("la-1-invalid-licence-date-format.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors.status", everyItem(equalTo(400)))
        .body("errors.title", everyItem(equalTo("Value error")))
        .body("errors.detail",
            containsInAnyOrder(
                "Invalid start date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY",
                "Invalid end date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY"))
        .body("errors.vrm", everyItem(equalTo("1289J")));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateOrder() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("la-1-invalid-licence-dates-ordering.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Start date must be before end date"))
        .body("errors[0].vrm", equalTo("1289J"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidVrm() {
    atTheBeginningBucketShouldBeEmpty();

    registerBySecondUploader("la-2-invalid-vrm.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Invalid format of VRM"))
        .body("errors[0].vrm", equalTo("A99A99A"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithVrmStartingWithZero() {
    atTheBeginningBucketShouldBeEmpty();

    registerBySecondUploader("la-2-zero-starting-vrm.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Invalid format of VRM"))
        .body("errors[0].vrm", equalTo("099A99A"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithManyErrors() {
    atTheBeginningBucketShouldBeEmpty();

    registerBySecondUploader("la-3-many-validation-errors.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors", hasSize(12));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEmptyTaxiOrPhv() {
    atTheBeginningBucketShouldBeEmpty();

    registerBySecondUploader("la-1-la-2-la-3-invalid-taxi-or-phv.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Mandatory field missing"))
        .body("errors[0].detail", equalTo("Missing taxi/PHV value"))
        .body("errors[0].vrm", equalTo("GQ87HDL"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithStartDateTooFarInPast() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("la-1-start-date-too-early.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Start date cannot be more than 20 years in the past"))
        .body("errors[0].vrm", equalTo("XQ25QVS"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEndDateTooFarInFuture() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("la-1-end-date-too-late.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("End date cannot be more than 20 years in the future"))
        .body("errors[0].vrm", equalTo("XQ25QVS"));

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithInvalidWheelchairAccessibleValues() {
    atTheBeginningBucketShouldBeEmpty();

    ValidatableResponse validatableResponse = registerByFirstUploader(
        "la-1-invalid-wheelchair-accessible.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails();
    checkWheelchairErrorOnIndexAndVrn(validatableResponse, 0, "XQ25QVS");
    checkInvalidEndDateFormatOnIndexAndVrn(validatableResponse);
    checkWheelchairErrorOnIndexAndVrn(validatableResponse, 2, "XQ25QVT");
    checkWheelchairErrorOnIndexAndVrn(validatableResponse, 3, "XQ25QVU");

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  void whenThereIsAttemptToRegisterXssLikeEntriesForLicensePlate() {
    registerByFirstUploader(
        "la-1-xss-license-plate.json")
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  void whenThereIsAttemptToRegisterXssLikeEntriesForDescription() {
    registerByFirstUploader(
        "la-1-xss-description.json")
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  @Override
  void andAllFailedFilesShouldBeRemovedFromS3() {
    // not applicable to registering via REST API
  }

  @Override
  void whenThereIsAttemptToRegisterLicensesWithddMMyyyyFormat() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("la-1-dd-MM-yyyy-format.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail",
            containsInAnyOrder(
                "Invalid start date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY",
                "Invalid end date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY"))
        .body("errors[0].vrm", isEmptyOrNullString());

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  void whenThereIsAttemptToRegisterLicencesByUnauthorisedLicensingAuthority() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("all-licensing-authorities.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Insufficient Permissions"))
        .body("errors[0].detail", equalTo(
            "You are not authorised to submit data for la-4"))
        .body("errors[0].vrm", isEmptyOrNullString());

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  void whenThereIsAttemptToUpdateLicensingAuthorityLockedByAnotherJob() {
    atTheBeginningBucketShouldBeEmpty();

    registerByFirstUploader("leeds-locked-licensing-authorities.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Licensing Authority Unavailability"))
        .body("errors[0].detail", equalTo(
            "Licence Authority is locked because it is being updated now by another Uploader"))
        .body("errors[0].vrm", isEmptyOrNullString());

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Test
  public void whenTooManyLicensesAreRegisteredThenACleanUpRequestIsCreated() {
    atTheBeginningBucketShouldBeEmpty();

    whenTooManyLicensesAreRegistered();
    // Too early, message is yet available
    thenAssertThatNoJobCleanUpRequestIsCreated();
    Awaitility.with()
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .atMost(jobCleanupRequestDelayInSeconds * 2, TimeUnit.SECONDS)
        .until(this::jobCleanupRequestIsCreated);

    thenBucketShouldBeNotEmpty();
  }

  @Test
  public void whenFairAmountOfLicensesAreRegisteredThenNoCleanUpRequestIsCreated() {
    atTheBeginningBucketShouldBeEmpty();

    whenFairAmountOfLicensesAreRegistered();
    thenAssertThatNoJobCleanUpRequestIsCreated();

    thenBucketShouldBeNotEmpty();
  }

  @Test
  public void shouldNotRegisterPayloadWithXssAttemptForLicensePlate() {
    atTheBeginningBucketShouldBeEmpty();

    whenThereIsAttemptToRegisterXssLikeEntriesForLicensePlate();

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }

  @Test
  public void shouldNotRegisterPayloadWithXssAttemptForDescription() {
    atTheBeginningBucketShouldBeEmpty();

    whenThereIsAttemptToRegisterXssLikeEntriesForDescription();

    thenBucketShouldBeEmptyAfterUnsuccessfulUpload();
  }


  private boolean jobCleanupRequestIsCreated() {
    return countJobCleanUpRequest() == 1;
  }

  private void thenAssertThatNoJobCleanUpRequestIsCreated() {
    assertThat(countJobCleanUpRequest()).isEqualTo(0);
  }

  private int countJobCleanUpRequest() {
    GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(QUEUE_NAME)
        .build();
    String sqsQueueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    ReceiveMessageResponse response = sqsClient
        .receiveMessage(ReceiveMessageRequest.builder().queueUrl(sqsQueueUrl).build());
    return response.messages().size();
  }

  private void checkInvalidEndDateFormatOnIndexAndVrn(ValidatableResponse validatableResponse) {
    checkErrorOnIndexAndVrn(validatableResponse, 1, "XQ25QVT",
        "Invalid end date format. Date format must be either ISO (YYYY-MM-DD) or DD/MM/YYYY");
  }

  private void checkWheelchairErrorOnIndexAndVrn(ValidatableResponse validatableResponse, int index,
      String vrn) {
    checkErrorOnIndexAndVrn(validatableResponse, index, vrn, "Invalid wheelchair accessible value."
        + " Can only be True or False");
  }

  private void checkErrorOnIndexAndVrn(ValidatableResponse validatableResponse, int index,
      String vrn, String expectedErrorMessage) {
    String errorsIndex = "errors[" + index + "]";
    validatableResponse
        .body(errorsIndex + ".status", equalTo(400))
        .body(errorsIndex + ".title", equalTo("Value error"))
        .body(errorsIndex + ".detail", equalTo(expectedErrorMessage))
        .body(errorsIndex + ".vrm", equalTo(vrn));
  }

  private void createBucket() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
  }

  private void deleteBucket() {
    S3Operation.clearBucket(s3Client, BUCKET_NAME);
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME).build());
  }

  private void atTheBeginningBucketShouldBeEmpty() {
    S3Operation.clearBucket(s3Client, BUCKET_NAME);
    S3Operation.bucketShouldBeEmpty(s3Client, BUCKET_NAME);
  }

  private void thenBucketShouldBeEmptyAfterUnsuccessfulUpload() {
    S3Operation.bucketShouldBeEmpty(s3Client, BUCKET_NAME);
  }

  private void thenBucketShouldBeNotEmpty() {
    ListObjectsResponse listObjectsResponse = S3Operation
        .getListObjectsResponse(s3Client, BUCKET_NAME);
    assertThat(listObjectsResponse.contents()).hasSize(1);
  }

  private RegisterByApiAssertion whenRegister(String jsonFilename) {
    return RegisterByApiAssertion.register(s3Client, objectMapper, jsonFilename, BUCKET_NAME);
  }

  private ValidatableResponse registerByFirstUploader(String jsonFilename) {
    return RegisterByApiAssertion.registerByFirstUploader(jsonFilename);
  }

  private ValidatableResponse registerBySecondUploader(String jsonFilename) {
    return RegisterByApiAssertion.registerBySecondUploader(jsonFilename);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class RegisterByApiAssertion {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String requestPayload;
    private final String bucketName;
    private List<TaxiPhvVehicleLicence> licences;
    private ListObjectsResponse listObjectsResponse;

    static RegisterByApiAssertion register(S3Client s3Client, ObjectMapper objectMapper,
        String requestPayload, String bucketName) {
      return new RegisterByApiAssertion(s3Client, objectMapper, requestPayload, bucketName);
    }

    static ValidatableResponse registerByFirstUploader(String jsonFilename) {
      return RegisterByApiAssertion.register(readFile(jsonFilename), FIRST_UPLOADER_ID);
    }

    static ValidatableResponse registerBySecondUploader(String jsonFilename) {
      return RegisterByApiAssertion.register(readFile(jsonFilename), SECOND_UPLOADER_ID);
    }

    static ValidatableResponse register(String payload, UUID uploaderId) {
      String correlationId = UUID.randomUUID().toString();
      return register(payload, uploaderId, correlationId)
          .header(CORRELATION_ID_HEADER, correlationId);
    }

    static ValidatableResponse register(String payload, UUID uploaderId, String correlationId) {
      return RestAssured
          .given()
          .accept(ContentType.JSON)
          .contentType(ContentType.JSON)
          .header(Constants.CORRELATION_ID_HEADER, correlationId)
          .header(Constants.API_KEY_HEADER, uploaderId)
          .body(payload)
          .when()
          .post("/taxiphvdatabase")
          .then();
    }

    @SneakyThrows
    static String readFile(String filename) {
      return Resources.toString(Resources.getResource("data/json/" + filename), Charsets.UTF_8);
    }

    private RegisterByApiAssertion thenRegisterByFirstUploader() {
      register(readFile(requestPayload), FIRST_UPLOADER_ID);
      return this;
    }

    private RegisterByApiAssertion thenRegisterBySecondUploader() {
      register(readFile(requestPayload), SECOND_UPLOADER_ID);
      return this;
    }

    private RegisterByApiAssertion atTheBeginningBucketShouldBeEmpty() {
      S3Operation.clearBucket(s3Client, bucketName);
      S3Operation.bucketShouldBeEmpty(s3Client, bucketName);
      return this;
    }

    private void thenBucketShouldBeEmptyAfterUnsuccessfulUpload() {
      S3Operation.bucketShouldBeEmpty(s3Client, bucketName);
    }

    @SneakyThrows
    private RegisterByApiAssertion thenBucketShouldBeNotEmpty() {
      listObjectsResponse = S3Operation.getListObjectsResponse(s3Client, bucketName);
      S3Object s3Object = listObjectsResponse.contents().get(0);
      GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName)
          .key(s3Object.key())
          .build();
      InputStream inputStream = s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
      licences = objectMapper
          .readValue(inputStream, new TypeReference<List<TaxiPhvVehicleLicence>>() {
          });
      return this;
    }

    private RegisterByApiAssertion andContainsOneKey() {
      assertThat(listObjectsResponse.contents()).hasSize(1);
      return this;
    }

    private void andJsonListWithSize(int size) {
      assertThat(licences).hasSize(size);
    }
  }

  private static class S3Operation {

    static void bucketShouldBeEmpty(S3Client s3Client, String bucketName) {
      ListObjectsResponse listObjectsResponse = getListObjectsResponse(s3Client, bucketName);
      assertThat(listObjectsResponse.contents()).hasSize(0);
    }

    static ListObjectsResponse getListObjectsResponse(S3Client s3Client, String bucketName) {
      ListObjectsRequest listObjectsRequest = getListObjectsRequest(bucketName);
      return s3Client.listObjects(listObjectsRequest);
    }

    static void clearBucket(S3Client s3Client, String bucketName) {
      ListObjectsResponse listObjectsResponse = S3Operation
          .getListObjectsResponse(s3Client, bucketName);
      listObjectsResponse.contents().forEach(s3Object -> {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName)
            .key(s3Object.key()).build();
        s3Client.deleteObject(deleteObjectRequest);
      });
    }

    private static ListObjectsRequest getListObjectsRequest(String bucketName) {
      return ListObjectsRequest.builder().bucket(bucketName).build();
    }
  }
}
