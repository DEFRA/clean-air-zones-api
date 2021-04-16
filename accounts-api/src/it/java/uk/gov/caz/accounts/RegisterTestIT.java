package uk.gov.caz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import joptsimple.internal.Strings;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.accounts.annotation.FullyRunningIntegrationTest;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.accounts.dto.RegisterJobStatusDto;
import uk.gov.caz.accounts.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.accounts.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.AccountUserPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.model.registerjob.RegisterJob;
import uk.gov.caz.accounts.model.registerjob.RegisterJobError;
import uk.gov.caz.accounts.model.registerjob.RegisterJobErrors;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.accounts.repository.AccountPermissionRepository;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserPermissionRepository;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.RegisterJobRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.ExternalCallsIT;
import uk.gov.caz.accounts.service.registerjob.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.correlationid.Constants;

/**
 * This class provides storage-specific methods for inserting VRNs.
 */
@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/registerjob/clear-jobs-and-vrns.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/registerjob/clear-jobs-and-vrns.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@TestPropertySource(properties = {
    "charge-calculation.lambda.max-vehicles-to-process=2",
    // to force spawning a new charge calc lambda/thread
})
public class RegisterTestIT extends ExternalCallsIT {

  private static final Path FILE_BASE_PATH = Paths
      .get("src", "it", "resources", "data", "csv", "registerjob");

  private static final UUID BIRMINGHAM_CAZ_ID = UUID
      .fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
  private static final UUID BATH_CAZ_ID = UUID.fromString("742b343f-6ce6-42d3-8324-df689ad4c515");

  private static final String BUCKET_NAME = String.format(
      "accounts-vehicles-data-%d",
      System.currentTimeMillis()
  );

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private AccountUserRepository accountUserRepository;

  @Autowired
  private AccountPermissionRepository accountPermissionRepository;

  @Autowired
  private AccountUserPermissionRepository accountUserPermissionRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RegisterJobRepository registerJobRepository;

  @Autowired
  private IdentityProvider identityProvider;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @Value("${application.validation.max-errors-count}")
  private int maxErrorsCount;

  @Autowired
  private AmazonSQS sqsClient;

  private Account account;
  private Account anotherAccount;
  private UserEntity firstUser;
  private UserEntity secondUser;
  private RegisterCsvFromS3JobHandle firstJobHandle;
  private RegisterCsvFromS3JobHandle secondJobHandle;
  private volatile StatusOfRegisterCsvFromS3JobQueryResult queryResult;

  private List<String> uploadedFiles = new ArrayList<>();
  private int expectedRegisterJobsCount = 0;

  @BeforeEach
  public void setUp() {
    createBucketInS3();
    setUpRestAssured();
    mockVccsCleanAirZonesCall();
  }

  @AfterEach
  public void tearDown() {
    deleteBucketAndFilesFromS3();
    clearAllTables();
  }

  @BeforeEach
  public void createEmailQueue() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
  }

  @AfterEach
  public void deleteQueue() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
  }

  private void clearAllTables() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "caz_account.t_account_user_permission",
        "caz_account.t_account_user", "caz_account.t_account_vehicle", "caz_account.t_account",
        "caz_account.t_account_job_register");
  }

  @Test
  public void registrationTest() {
    givenThereIsAccount();
    andAssociatedFirstAccountUser();
    andAssociatedSecondAccountUser();
    andCsvFileHasBeenUploadedToS3ByUser("first-uploader-records-all.csv", firstUser);
    andCsvFileHasBeenUploadedToS3ByUser("first-uploader-records-with-duplicates.csv", firstUser);
    andCsvFileHasBeenUploadedToS3ByUser("second-uploader-max-validation-errors-exceeded.csv",
        secondUser);
    andCsvFileHasBeenUploadedToS3ByUser(
        "second-uploader-mixed-parse-and-business-validation-errors.csv",
        secondUser);

    // first uploader - contains duplicated VRNs
    whenVehiclesAreRegisteredByFirstUploader("first-uploader-records-with-duplicates.csv");
    thenRegisterJobShouldBePresentInTheDatabase();
    thenNoVehiclesShouldBeRegisteredByFirstUploader();
    andJobShouldFinishWithFailureStatus();
    andJobContainsVehiclesDuplicationErrors();
    andNoEmailsHaveBeenSent();

    // first uploader test
    // ABC459 does not exist in DVLa and its compliance cannot be determined
    mockVccsBulkComplianceCallForVrnsFromRequestExceptFor(BIRMINGHAM_CAZ_ID, BATH_CAZ_ID,
        Collections.singleton("ABC459"));
    atTheBeginningThereShouldBeNoVehicles();
    whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader("first-uploader-records-all.csv");
    thenRegisterJobShouldBePresentInTheDatabase();
    thenAllShouldBeInserted();
    andChargeabilityCacheShouldBePopulatedForAllVehiclesOf(account.getId());
    andChargeabilityCacheWithNonCompliantDataShouldBePopulatedFor("ABC459");
    andChargeabilityCacheWithCompliantDataShouldBePopulatedFor("ABC456", "ABC457", "ABC458");
    andThereShouldBeNoErrors();
    andEmailsToVehicleManagersHaveBeenSent();

    // rerun test - there should be no errors
    whenVehiclesAreRegisteredByFirstUploaderWithSendEmailsSetToFalse(
        "first-uploader-records-all.csv");
    thenRegisterJobShouldBePresentInTheDatabase();
    thenAllShouldBeInserted();
    andThereShouldBeNoErrors();
    andNoEmailsHaveBeenSent();

    // second uploader test
    whenVehiclesAreRegisteredBySecondUploader("second-uploader-max-validation-errors-exceeded.csv");
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobContainsSpecificErrors();
    andJobShouldFinishWithFailureStatus();
    andThereShouldBeMaxValidationErrors();
    andNoEmailsHaveBeenSent();

    whenVehiclesAreRegisteredBySecondUploader(
        "second-uploader-mixed-parse-and-business-validation-errors.csv");
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobContainsMixedParseAndBusinessValidationErrors();
    andJobShouldFinishWithFailureStatus();
    andThereShouldBeMaxValidationErrors();
    andFailedFileShouldBeRemovedFromS3();
    andNoEmailsHaveBeenSent();
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.accounts.RegisterTestIT#filesWithoutVehicles")
  public void emptyUploadTest(String emptyFilename) {
    mockVccsBulkComplianceCallForVrnsFromRequest(BIRMINGHAM_CAZ_ID, BATH_CAZ_ID);

    givenThereIsAccount();
    andAssociatedFirstAccountUser();
    givenThereIsAnotherAccount();
    andAssociatedSecondAccountUserTo(anotherAccount);
    andCsvFileHasBeenUploadedToS3ByUser("first-uploader-records-all.csv", firstUser);
    andCsvFileHasBeenUploadedToS3ByUser("second-uploader-records-all.csv", secondUser);
    andVehiclesHaveBeenRegisteredFromNonEmptyFiles();
    andCsvFileHasBeenUploadedToS3ByUser(emptyFilename, firstUser);

    whenVehiclesAreRegisteredByFirstUploader(emptyFilename);
    thenDatabaseShouldContainAllOriginalRecords();
    andResponseShouldHaveAppropriateMessage("Your CSV did not contain any number plates. "
        + "Add your number plates and upload it again.");
  }

  private void andNoEmailsHaveBeenSent() {
    Optional<Message> message = receiveSqsMessage();
    assertThat(message).isEmpty();
  }

  private void andEmailsToVehicleManagersHaveBeenSent() {
    Optional<Message> firstMessage = receiveSqsMessage();
    Optional<Message> secondMessage = receiveSqsMessage();

    assertThat(firstMessage).isPresent();
    assertThat(secondMessage).isPresent();
    List<String> recipients = Stream.of(firstMessage, secondMessage)
        .map(Optional::get)
        .map(message -> {
          try {
            return objectMapper
                .readValue(message.getBody(), new TypeReference<Map<String, Object>>() {
                });
          } catch (JsonProcessingException e) {
            return Collections.emptyMap();
          }
        })
        .map(message -> (String) message.get("emailAddress"))
        .collect(Collectors.toList());
    assertThat(recipients).containsExactlyInAnyOrder(firstUser.getEmail(), secondUser.getEmail());
  }

  private void andJobContainsVehiclesDuplicationErrors() {
    List<String> errors = getJobErrorsByJobName(firstJobHandle.getJobName());
    assertThat(errors).containsExactly(
        "Line 4: There are multiple vehicles with the same VRN",
        "Line 6: There are multiple vehicles with the same VRN",
        "Line 8: There are multiple vehicles with the same VRN"
    );
  }

  private void andVehiclesHaveBeenRegisteredFromNonEmptyFiles() {
    registerVehiclesFromForFirstUploader("first-uploader-records-all.csv", true);
    awaitForJobToFinish(firstJobHandle);
    registerVehiclesForSecondUploader("second-uploader-records-all.csv");
    awaitForJobToFinish(secondJobHandle);
  }

  private void thenDatabaseShouldContainAllOriginalRecords() {
    int vehiclesCount = getUploaderVehiclesCount(firstUser.getAccountId())
        + getUploaderVehiclesCount(secondUser.getAccountId());
    assertThat(countAllVehicles()).isEqualTo(vehiclesCount);
  }

  private void andResponseShouldHaveAppropriateMessage(String error) {
    List<String> errors = getJobErrorsByJobName(firstJobHandle.getJobName());
    assertTrue(errors.contains(error));
  }

  private void andAssociatedSecondAccountUserTo(Account account) {
    UserEntity randomUserForAccount = createRandomUserForAccount(2, account);
    secondUser = userRepository.save(randomUserForAccount);
    saveUserInIdentityProvider(randomUserForAccount);
  }

  private UserEntity createRandomUserForAccount(int userNo, Account account) {
    return createRandomUser(userNo, account.getId());
  }

  private int getUploaderVehiclesCount(UUID accountId) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_vehicle",
        String.format("account_id = '%s'", accountId));
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.accounts.RegisterTestIT#activeJobStatuses")
  public void simultaneousRegisterPreventionTest(RegisterJobStatus status) {
    givenThereIsAccount();
    andAssociatedFirstAccountUser();
    andCsvFileHasBeenUploadedToS3ByUser("first-uploader-records-all.csv", firstUser);
    andThereIsRegisterJobWithSameAccountIdAndStateEqualTo(status);

    whenVehiclesAreRegisteredThereShouldBe406StatusCodeReturned("first-uploader-records-all.csv");
    andNoRegisterJobsShouldBeAddedToDatabase();
  }

  private void whenVehiclesAreRegisteredByFirstUploaderWithSendEmailsSetToFalse(String filename) {
    registerVehiclesFromForFirstUploader(filename, false);
    awaitForJobToFinish(firstJobHandle);
  }

  private void whenVehiclesAreRegisteredByFirstUploader(String filename) {
    registerVehiclesFromForFirstUploader(filename, true);
    awaitForJobToFinish(firstJobHandle);
  }

  private void andThereShouldBeMaxValidationErrors() {
    assertThat(queryResult.getErrors()).isNotNull();
    assertThat(queryResult.getErrors()).hasSize(maxErrorsCount);
  }

  void andFailedFileShouldBeRemovedFromS3() {
    verifyFileHasBeenRemovedFromS3("second-uploader-max-validation-errors-exceeded.csv");
    verifyFileHasBeenRemovedFromS3("first-uploader-records-with-duplicates.csv");
    verifyFileHasBeenRemovedFromS3(
        "second-uploader-mixed-parse-and-business-validation-errors.csv");
  }

  private void verifyFileHasBeenRemovedFromS3(String filename) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key(filename)
        .build();

    Throwable throwable = catchThrowable(() -> s3Client.getObject(getObjectRequest));

    assertThat(throwable).isInstanceOf(NoSuchKeyException.class);
  }

  private void andJobShouldFinishWithFailureStatus() {
    assertThat(queryResult.getStatus()).isEqualTo(RegisterJobStatusDto.FAILURE);
  }

  private void andJobContainsSpecificErrors() {
    List<String> errors = getJobErrorsByJobName(secondJobHandle.getJobName());
    assertThat(errors).containsExactly(
        "Line 2: Number plates need to be between 2 and 7 characters instead of 15.",
        "Line 3: Number plates need to be between 2 and 7 characters instead of 24.",
        "Line 4: abc-vrn - this number plate is not in a valid format.",
        "Line 5: //com - this number plate is not in a valid format.",
        "Line 6: is empty.",
        "Line 7: źŁĄćW34 - this number plate is not in a valid format.",
        "Line 8: is empty.",
        "Line 9: Your file should only have 1 column of number plates."
    );
  }

  private void andJobContainsMixedParseAndBusinessValidationErrors() {
    List<String> errors = getJobErrorsByJobName(secondJobHandle.getJobName());
    assertThat(errors).containsExactly(
        "Line 3: is empty.",
        "Line 7: is empty.",
        "Line 11: is empty.",
        "Line 15: is empty.",
        "Line 37: Set-2 - this number plate is not in a valid format.",
        "Line 54: Number plates need to be between 2 and 7 characters instead of 1.",
        "Line 55: is empty.",
        "Line 125: Set-4 - this number plate is not in a valid format."
    );
  }

  private List<String> getJobErrorsByJobName(String jobName) {
    return registerJobRepository
        .findByJobName(new RegisterJobName(jobName))
        .map(RegisterJob::getErrors)
        .map(RegisterJobErrors::getErrors)
        .map(registerJobErrors -> registerJobErrors.stream()
            .map(RegisterJobError::getDetail)
            .collect(Collectors.toList()))
        .orElseThrow(() -> new IllegalStateException("Can't find the job"));
  }

  private void whenVehiclesAreRegisteredBySecondUploader(String filename) {
    registerVehiclesForSecondUploader(filename);
    awaitForJobToFinish(secondJobHandle);
  }

  private void thenNoVehiclesShouldBeRegisteredBySecondUploader() {
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void thenNoVehiclesShouldBeRegisteredByFirstUploader() {
    assertThat(countAllVehiclesRegisteredByFistUploader()).isZero();
  }

  private void thenAllShouldBeInserted() {
    awaitForJobToFinish(firstJobHandle);
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void awaitForJobToFinish(RegisterCsvFromS3JobHandle handle) {
    Awaitility.with()
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to finish")
        .atMost(6, TimeUnit.SECONDS)
        .until(() -> jobHasFinished(handle));
  }

  private boolean jobHasFinished(RegisterCsvFromS3JobHandle jobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryResult = getJobInfo(jobHandle.getJobName());
    this.queryResult = queryResult;
    return queryResult.getStatus() != RegisterJobStatusDto.RUNNING
        && queryResult.getStatus() != RegisterJobStatusDto.CHARGEABILITY_CALCULATION_IN_PROGRESS;
  }

  private StatusOfRegisterCsvFromS3JobQueryResult getJobInfo(String jobName) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured.given()
        .accept(ContentType.JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .get("/register-csv-from-s3/jobs/{registerJobName}",
            jobName)
        .then()
        .statusCode(HttpStatus.OK.value())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .extract().as(StatusOfRegisterCsvFromS3JobQueryResult.class);
  }

  private void andThereShouldBeNoErrors() {
    boolean hasErrors = queryResult.getErrors() == null || queryResult.getErrors().length == 0;
    assertThat(hasErrors).isTrue();
  }

  private void allVehiclesInDatabaseAreInsertedByFirstUploader() {
    assertThat(countAllVehicles()).isEqualTo(4);
  }

  public static Stream<RegisterJobStatus> activeJobStatuses() {
    return Stream.of(RegisterJobStatus.RUNNING, RegisterJobStatus.STARTING);
  }

  public static Stream<String> filesWithoutVehicles() {
    return Stream.of("empty-file.csv", "file-with-only-header.csv");
  }

  private void andNoRegisterJobsShouldBeAddedToDatabase() {
    int actualJobsCount = JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_account.t_account_job_register");
    assertThat(actualJobsCount).isEqualTo(expectedRegisterJobsCount);
  }

  private void whenVehiclesAreRegisteredThereShouldBe406StatusCodeReturned(String filename) {
    initJob(filename)
        .then()
        .statusCode(HttpStatus.NOT_ACCEPTABLE.value());
  }

  private void andThereIsRegisterJobWithSameAccountIdAndStateEqualTo(RegisterJobStatus status) {
    RegisterJob registerJob = RegisterJob.builder()
        .trigger(RegisterJobTrigger.CSV_FROM_S3)
        .uploaderId(firstUser.getAccountId())
        .status(status)
        .jobName(new RegisterJobName("some-job"))
        .correlationId(UUID.randomUUID().toString())
        .errors(new RegisterJobErrors(Collections.emptyList()))
        .build();
    registerJobRepository.save(registerJob);
    expectedRegisterJobsCount += 1;
  }

  private void andCsvFileHasBeenUploadedToS3ByUser(String filename, UserEntity user) {
    uploadFileToS3(user.getId().toString(), filename);
  }

  private void givenThereIsAccount() {
    Account accountToBeCreated = Account.builder()
        .name("test-account")
        .build();
    account = accountRepository.save(accountToBeCreated);
  }

  private void givenThereIsAnotherAccount() {
    Account accountToBeCreated = Account.builder()
        .name("test-account-2")
        .build();
    anotherAccount = accountRepository.save(accountToBeCreated);
  }

  private void andAssociatedFirstAccountUser() {
    firstUser = userRepository.save(createRandomUser(1, account.getId()));
    accountUserPermissionRepository
        .save(createVehicleManagementAccountPermission(firstUser.getId()));
    saveUserInIdentityProvider(firstUser);
  }

  private void andAssociatedSecondAccountUser() {
    secondUser = userRepository.save(createRandomUser(2, account.getId()));
    accountUserPermissionRepository
        .save(createVehicleManagementAccountPermission(secondUser.getId()));
    saveUserInIdentityProvider(secondUser);
  }

  private void saveUserInIdentityProvider(UserEntity user) {
    identityProvider.createAdminUser(
        user.getIdentityProviderUserId(),
        user.getEmail(),
        "abc"
    );
  }

  private UserEntity createRandomUser(int userNo, UUID accountId) {
    return UserEntity.builder()
        .email("user-" + userNo + "@defra.gov.uk")
        .identityProviderUserId(UUID.randomUUID())
        .accountId(accountId)
        .build();
  }

  private AccountUserPermission createVehicleManagementAccountPermission(UUID userId) {
    AccountPermission accountPermissionManageVehicles = accountPermissionRepository
        .findByName(Permission.MANAGE_VEHICLES).get();
    return AccountUserPermission.builder()
        .accountPermissionId(accountPermissionManageVehicles.getId())
        .accountUserId(userId)
        .build();
  }

  @SneakyThrows
  private Optional<Message> receiveSqsMessage() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
        .withMaxNumberOfMessages(1)
        .withQueueUrl(queueUrlResult.getQueueUrl());
    ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
    List<Message> messages = receiveMessageResult.getMessages();

    if (messages.isEmpty()) {
      return Optional.empty();
    }

    Message message = messages.iterator().next();
    sqsClient.deleteMessage(queueUrlResult.getQueueUrl(), message.getReceiptHandle());
    return Optional.of(message);
  }

  private void atTheBeginningThereShouldBeNoVehicles() {
    assertThat(countAllVehicles()).isEqualTo(0);
  }

  private void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader(String filename) {
    registerVehiclesFromForFirstUploader(filename, true);
  }

  private void registerVehiclesFromForFirstUploader(String filename, boolean shouldSendEmails) {
    firstJobHandle = startJob(filename, shouldSendEmails);
  }

  private void registerVehiclesForSecondUploader(String filename) {
    secondJobHandle = startJob(filename, true);
  }

  private void thenRegisterJobShouldBePresentInTheDatabase() {
    int jobsCount = getJobsCountByJobName(firstJobHandle.getJobName());
    assertThat(jobsCount).isEqualTo(1);
  }

  private int getJobsCountByJobName(String jobName) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account_job_register",
        "job_name = '" + jobName + "'");
  }

  private RegisterCsvFromS3JobHandle startJob(String filename, boolean shouldSendEmails) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(preparePayload(filename, shouldSendEmails))
        .when()
        .post("/register-csv-from-s3/jobs")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .extract().as(RegisterCsvFromS3JobHandle.class);
  }

  private Response initJob(String filename) {
    return RestAssured.given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .body(preparePayload(filename, null))
        .when()
        .post("/register-csv-from-s3/jobs");
  }

  @SneakyThrows
  private String preparePayload(String filename, Boolean shouldSendEmails) {
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(BUCKET_NAME,
        filename, shouldSendEmails);
    return objectMapper.writeValueAsString(cmd);
  }

  private void createBucketInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
  }

  private void deleteBucketAndFilesFromS3() {
    deleteFilesFromS3(uploadedFiles);
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  private void uploadFileToS3(String uploaderId, String filename) {
    s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
            .key(filename)
            .metadata(
                ImmutableMap.of(
                    CsvFileOnS3MetadataExtractor.ACCOUNT_USER_ID_METADATA_KEY, uploaderId
                )
            ),
        FILE_BASE_PATH.resolve(filename)
    );
    uploadedFiles.add(filename);
  }

  private void deleteFilesFromS3(List<String> filenames) {
    for (String filename : filenames) {
      s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(filename));
    }
  }

  private void setUpRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/accounts";
  }

  private int countAllVehicles() {
    return JdbcTestUtils.countRowsInTable(jdbcTemplate, "caz_account.t_account_vehicle");
  }

  private int countAllVehiclesRegisteredByFistUploader() {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_vehicle",
        "account_id = '" + firstUser.getAccountId() + "'");
  }

  private void andChargeabilityCacheShouldBePopulatedForAllVehiclesOf(UUID accountId) {
    int differentCazCount = 2;
    int accountVehiclesCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account_vehicle", "account_id = '" + accountId.toString() + "'");
    int chargeabilityCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_vehicle_chargeability vc, caz_account.t_account_vehicle av",
        "av.account_vehicle_id = vc.account_vehicle_id "
            + "and av.account_id = '" + accountId.toString() + "'");
    assertThat(chargeabilityCount).isEqualTo(accountVehiclesCount * differentCazCount);
  }

  private void andChargeabilityCacheWithCompliantDataShouldBePopulatedFor(String... vrns) {
    int differentCazCount = 2;
    int chargeabilityCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_vehicle_chargeability vc, caz_account.t_account_vehicle av",
        "av.account_vehicle_id = vc.account_vehicle_id "
            + "and vc.charge is not null "
            + "and vc.tariff_code is not null "
            + "and av.vrn in (" + toCommaSeparatedString(vrns) + ")");
    assertThat(chargeabilityCount).isEqualTo(vrns.length * differentCazCount);
  }

  private void andChargeabilityCacheWithNonCompliantDataShouldBePopulatedFor(String... vrns) {
    int differentCazCount = 2;
    int chargeabilityCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_vehicle_chargeability vc, caz_account.t_account_vehicle av",
        "av.account_vehicle_id = vc.account_vehicle_id "
            + "and vc.charge is null "
            + "and vc.tariff_code is null "
            + "and av.vrn in (" + toCommaSeparatedString(vrns) + ")");
    assertThat(chargeabilityCount).isEqualTo(vrns.length * differentCazCount);
  }

  private String toCommaSeparatedString(String[] vrns) {
    List<String> sqlString = Arrays.stream(vrns)
        .map(v -> "'" + v + "'")
        .collect(Collectors.toList());
    return Strings.join(sqlString, ", ");
  }

}
