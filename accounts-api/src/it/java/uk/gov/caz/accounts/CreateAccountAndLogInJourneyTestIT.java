package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
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
import uk.gov.caz.accounts.assertion.LoginUserJourneyAssertion;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateAccountAndLogInJourneyTestIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AmazonSQS sqsClient;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @BeforeEach
  public void setupRestAssured() {
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
  public void createAndLogInIntoAccountTest() {
    String accountName = "my-account";
    String email = "my-account@my-mail.com";
    String password = "my-secret-password";
    String verificationUrl = "http://example.com";

    AccountCreationResponseDto creationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    UserCreationResponseDto accountCreationResponse = givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword(password)
        .andVerificationUrl(verificationUrl)
        .forAccountId(creationResponseDto.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .accountUserIsCreatedInDatabaseWithInsertTimeStamp()
        .accountUserIsCreatedInDatabaseWithEmptySignInTimeStamp()
        .adminUserIsCreatedInDatabase();

    givenLoginUserJourney()
        .forEmail(email)
        .andPassword(password)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode()
        .accountUserIsUpdatedInDatabaseWithSignInTimeStamp()
        .withAccountNameEqualTo(accountName)
        .withEmailEqualTo(email)
        .withAccountIdEqualTo(accountCreationResponse.getAccountId())
        .withAccountUserIdEqualTo(accountCreationResponse.getAccountUserId())
        .withAdminEqualToTrue()
        .withPasswordUpdateTimestamp(LocalDateTime.of(2020, 1, 10, 5, 28, 48));

    String invalidPassword = password + "b";
    givenLoginUserJourney()
        .forEmail(email)
        .andPassword(invalidPassword)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpUnauthorisedStatusCode();
  }

  @Test
  public void testCannotCreateAccountWhenThereAreUsersPending() {
    String accountName = "testaccount-1";
    String email = "my-account3@my-mail-1.com";
    String password = "my-secret-password";
    String verificationUrl = "verurl";

    AccountCreationResponseDto creationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword(password)
        .andVerificationUrl(verificationUrl)
        .forAccountId(creationResponseDto.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .adminUserIsCreatedInDatabase();

    givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageEqualTo("The company name already exists. There are users pending.");
  }

  @Test
  public void testNotExistingAccount() {
    String notExistingUser = "i-do-not-exist@domain.com";
    String password = "my-secret-password";

    givenLoginUserJourney()
        .forEmail(notExistingUser)
        .andPassword(password)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpUnauthorisedStatusCode();
  }

  @Test
  public void testAccountNameReuseForDeactivatedAccount() {
    String accountName = "testaccount";
    String email = "my-account2@my-mail.com";
    String password = "my-secret-password";
    String verificationUrl = "verurl";

    AccountCreationResponseDto creationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    String accountUserId = givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword(password)
        .andVerificationUrl(verificationUrl)
        .forAccountId(creationResponseDto.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .adminUserIsCreatedInDatabase()
        .getAccountUserId();

    deactivateAccount(accountUserId);

    givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode();
  }

  @Test
  public void testAccountNameProfanityDetection() {
    String accountName = "I'm an asshole";

    givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageEqualTo("Improper language detected")
        .andErrorCodeEqualTo("profanity");
  }

  @Test
  public void testAccountNameAbuseDetection() {
    String accountName = "idiot";

    givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageEqualTo("Improper language detected")
        .andErrorCodeEqualTo("abuse");
  }

  private void deactivateAccount(String accountUserId) {
    jdbcTemplate.update("UPDATE CAZ_ACCOUNT.T_ACCOUNT_USER SET user_id = null where account_user_id = ?", preparedStatement -> preparedStatement.setObject(1,
        UUID.fromString(accountUserId)));
    jdbcTemplate.update("UPDATE CAZ_ACCOUNT.T_ACCOUNT_USER_CODE SET status = 'USED' where account_user_id = ?", preparedStatement -> preparedStatement.setObject(1,
        UUID.fromString(accountUserId)));
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private LoginUserJourneyAssertion givenLoginUserJourney() {
    return new LoginUserJourneyAssertion(objectMapper, jdbcTemplate);
  }
}
