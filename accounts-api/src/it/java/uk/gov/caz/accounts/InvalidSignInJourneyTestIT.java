package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.util.Collections;
import org.json.JSONException;
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
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class InvalidSignInJourneyTestIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AmazonSQS sqsClient;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private IdentityProvider identityProvider;

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
  public void signInWithInvalidPasswordTest() throws JSONException {
    String email = "test@email.com";
    String password = "my-secret-password";
    String invalidPassword = "invalid-password";

    registerUserAccount(email, password);

    // Then User cannot login with invalid password.
    givenLoginUserJourney()
        .forEmail(email)
        .andPassword(invalidPassword)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpUnauthorisedStatusCode()
        .accountUserIsNotUpdatedInDatabaseWithSignInTimeStamp();
  }

  private void registerUserAccount(String email, String password) {
    String accountName = "my-account";
    String verificationUrl = "http://example.com";

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
        .whenRequestToCreateAdminUserIsMade();
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
