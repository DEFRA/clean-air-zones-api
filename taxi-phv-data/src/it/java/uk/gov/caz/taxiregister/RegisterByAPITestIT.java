package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import uk.gov.caz.taxiregister.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.taxiregister.controller.Constants;

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

  @Autowired
  private SqsClient sqsClient;
  
  @Value("${application.job-clean-up-request.message-visibility-delay-in-seconds}")
  private int jobCleanupRequestDelayInSeconds;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  private void createSqsQueue() {
    CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(QUEUE_NAME).build();
    CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
  }

  @AfterEach
  private void deleteSqsQueue() {
    GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build();
    String sqsQueueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder().queueUrl(sqsQueueUrl).build();
    sqsClient.deleteQueue(deleteQueueRequest);
  }
  
  @Override
  int getServerPort() {
    return randomServerPort;
  }

  @Override
  void whenThreeLicencesAreUpdatedByFirstLicensingAuthority() {
    registerByFirstUploader("la-1-all-and-three-modified.json")
        .statusCode(HttpStatus.CREATED.value());
  }

  @Override
  void whenLicencesAreRegisteredFromSecondLicensingAuthorityWithNoDataFromTheFirstOne() {
    registerBySecondUploader("la-2-all.json").statusCode(HttpStatus.CREATED.value());
  }

  @Override
  void whenThereAreFiveLicencesLessRegisteredByFirstAndSecondLicensingAuthority() {
    registerByFirstUploader("la-1-la-2-all-but-five-less-for-each-la.json")
        .statusCode(HttpStatus.CREATED.value());
  }

  @Override
  void whenLicencesAreRegisteredFromFirstLicensingAuthorityAgainstEmptyDatabase() {
    registerByFirstUploader("la-1-all.json").statusCode(HttpStatus.CREATED.value());
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
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidLicenceDateOrder() {
    registerByFirstUploader("la-1-invalid-licence-dates-ordering.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Start date must be before end date"))
        .body("errors[0].vrm", equalTo("1289J"));
  }

  @Override
  void whenThereIsAttemptToRegisterLicenceWithInvalidVrm() {
    registerBySecondUploader("la-2-invalid-vrm.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Value error"))
        .body("errors[0].detail", equalTo("Invalid format of VRM"))
        .body("errors[0].vrm", equalTo("A99A99A"));
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithTooManyErrors() {
    registerBySecondUploader("la-3-max-validation-errors-exceeded.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors", hasSize(10));
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEmptyTaxiOrPhv() {
    registerBySecondUploader("la-1-la-2-la-3-invalid-taxi-or-phv.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Mandatory field missing"))
        .body("errors[0].detail", equalTo("Missing taxi/PHV value"))
        .body("errors[0].vrm", equalTo("GQ87HDL"));
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithStartDateTooFarInPast() {
    registerByFirstUploader("la-1-start-date-too-early.json")
    .statusCode(HttpStatus.BAD_REQUEST.value())
    .log().ifValidationFails()
    .body("errors[0].status", equalTo(400))
    .body("errors[0].title", equalTo("Value error"))
    .body("errors[0].detail", equalTo("Start date cannot be more than 20 years in the past"))
    .body("errors[0].vrm", equalTo("XQ25QVS"));
  }

  @Override
  void whenThereIsAttemptToRegisterLicencesWithEndDateTooFarInFuture() {
    registerByFirstUploader("la-1-end-date-too-late.json")
    .statusCode(HttpStatus.BAD_REQUEST.value())
    .log().ifValidationFails()
    .body("errors[0].status", equalTo(400))
    .body("errors[0].title", equalTo("Value error"))
    .body("errors[0].detail", equalTo("End date cannot be more than 20 years in the future"))
    .body("errors[0].vrm", equalTo("XQ25QVS"));
  }

  @Override
  void andAllFailedFilesShouldBeRemovedFromS3() {
    // not applicable to registering via REST API
  }

  @Override
  void whenThereIsAttemptToRegisterLicensesWithddMMyyyyFormat() {
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
  }

  void whenThereIsAttemptToRegisterLicencesByUnauthorisedLicensingAuthority() {
    registerByFirstUploader("all-licensing-authorities.json")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .log().ifValidationFails()
        .body("errors[0].status", equalTo(400))
        .body("errors[0].title", equalTo("Insufficient Permissions"))
        .body("errors[0].detail", equalTo(
            "You are not authorised to submit data for la-4"))
        .body("errors[0].vrm", isEmptyOrNullString());
  }
  
  void whenThereIsAttemptToUpdateLicensingAuthorityLockedByAnotherJob() {
    registerByFirstUploader("leeds-locked-licensing-authorities.json")
    .statusCode(HttpStatus.BAD_REQUEST.value())
    .log().ifValidationFails()
    .body("errors[0].status", equalTo(400))
    .body("errors[0].title", equalTo("Licensing Authority Unavailability"))
    .body("errors[0].detail", equalTo(
        "Licence Authority is locked because it is being updated now by another Uploader"))
    .body("errors[0].vrm", isEmptyOrNullString());
  }
  
  private ValidatableResponse registerByFirstUploader(String jsonFilename) {
    return register(readFile(jsonFilename), FIRST_UPLOADER_ID);
  }

  private ValidatableResponse registerBySecondUploader(String jsonFilename) {
    return register(readFile(jsonFilename), SECOND_UPLOADER_ID);
  }

  private ValidatableResponse register(String payload, UUID uploaderId) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(Constants.CORRELATION_ID_HEADER, correlationId)
        .header(Constants.API_KEY_HEADER, uploaderId)
        .body(payload)
        .when()
        .post("/taxiphvdatabase")
        .then()
        .header(CORRELATION_ID_HEADER, correlationId);
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/json/" + filename), Charsets.UTF_8);
  }
  
  @Test
  public void whenTooManyLicensesAreRegisteredThenACleanUpRequestIsCreated() {
    whenTooManyLicensesAreRegistered();
    // Too early, message is yet available
    thenAssertThatNoJobCleanUpRequestIsCreated();
    Awaitility.with()
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .atMost(jobCleanupRequestDelayInSeconds * 2 , TimeUnit.SECONDS)
        .until(this::jobCleanupRequestIsCreated);
  }
  
  @Test
  public void whenFairAmountOfLicensesAreRegisteredThenNoCleanUpRequestIsCreated() {
    whenFairAmountOfLicensesAreRegistered();
    thenAssertThatNoJobCleanUpRequestIsCreated();
  }

  
  private boolean jobCleanupRequestIsCreated() {
   return countJobCleanUpRequest() == 1;
  }
  
  private void thenAssertThatNoJobCleanUpRequestIsCreated() {
    assertThat(countJobCleanUpRequest()).isEqualTo(0);
  }
  
  private int countJobCleanUpRequest() {
    GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build();
    String sqsQueueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(sqsQueueUrl).build());
    return response.messages().size();
  }
}
