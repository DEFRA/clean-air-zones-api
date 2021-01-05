package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.FullyRunningIntegrationTest;
import uk.gov.caz.accounts.assertion.CreateAccountJourneyAssertion;
import uk.gov.caz.accounts.assertion.CreateAdminUserJourneyAssertion;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateAdminUserTestIT {

  @LocalServerPort
  int randomServerPort;

  private final String VALID_ACCOUNT_NAME = "jaquza";
  private final String VALID_EMAIL = "jaqu@dev.co.uk";
  private final String VALID_PASSWORD = "Password.123";
  private final String VALID_VERIFICATION_URL = "http://example.com";

  @Autowired
  private AccountUserCodeRepository accountUserCodeRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AmazonSQS sqsClient;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @BeforeEach
  public void startMockServer() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
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

  @Test
  public void createMultipleAdminUserAccountJourney() {
    String accountId = createAccount(VALID_ACCOUNT_NAME);
    String validEmail1 = "jaqu2@dev.co.uk";
    String validEmail2 = "jaqu3@dev.co.uk";

    givenCreateAdminUserJourney()
        .forAccountId(accountId)
        .andEmail(validEmail1)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userEmailVerificationUrlHasBeenSentToTheUser()
        .verificationAccountUserCodeIsCreatedInTheDatabase()
        .accountUserIsCreatedInDatabaseWithInsertTimeStamp()
        .userIsCreatedWithThisPermissionsCount(5)
        .adminUserIsCreatedInDatabase();

    // Another admin user for the same account does not have all permissions set
    givenCreateAdminUserJourney()
        .forAccountId(accountId)
        .andEmail(validEmail2)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userIsCreatedWithThisPermissionsCount(5);
  }

  @Test
  public void createAdminUserAccountJourney() {
    String accountId = createAccount(VALID_ACCOUNT_NAME);

    givenCreateAdminUserJourney()
        .forAccountId(accountId)
        .andEmail(VALID_EMAIL)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userEmailVerificationUrlHasBeenSentToTheUser()
        .verificationAccountUserCodeIsCreatedInTheDatabase()
        .accountUserIsCreatedInDatabaseWithInsertTimeStamp()
        .userIsCreatedWithThisPermissionsCount(5)
        .adminUserIsCreatedInDatabase();

    // Duplicate email prevention for non-expired account
    String ANOTHER_VALID_ACCOUNT_NAME = "Funky Pigeon";
    String anotherAccountId = createAccount(ANOTHER_VALID_ACCOUNT_NAME);

    givenCreateAdminUserJourney()
        .forAccountId(anotherAccountId)
        .andEmail(VALID_EMAIL)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageEqualTo("User with given email already exists.");

    // Duplicated user for expired verification token - can be created
    makeAllAccountUserCodesExpired();
    givenCreateAdminUserJourney()
        .forAccountId(anotherAccountId)
        .andEmail(VALID_EMAIL)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userEmailVerificationUrlHasBeenSentToTheUser()
        .verificationAccountUserCodeIsCreatedInTheDatabase()
        .accountUserIsCreatedInDatabaseWithInsertTimeStamp()
        .adminUserIsCreatedInDatabase();
  }

  @Test
  public void responsesWith404WhenAccountDoesNotExist() {
    String nonExistingAccountId = "d623f0ba-978d-447a-9285-9dbba5109a1a";

    givenCreateAdminUserJourney()
        .forAccountId(nonExistingAccountId)
        .andEmail(VALID_EMAIL)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpNotFoundCode()
        .adminUserIsNotCreatedInDatabase();
  }

  private String createAccount(String accountName) {
    AccountCreationResponseDto creationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();
    return creationResponseDto.getAccountId();
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private void makeAllAccountUserCodesExpired() {
    Iterable<AccountUserCode> codes = accountUserCodeRepository.findAll();
    codes.forEach(code -> {
      code.setExpiration(LocalDateTime.now().minusDays(1));
      accountUserCodeRepository.save(code);
    });
  }
}
