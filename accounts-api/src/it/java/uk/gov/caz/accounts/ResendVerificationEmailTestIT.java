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
import uk.gov.caz.accounts.assertion.ResendVerificationEmailJourneyAssertion;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class ResendVerificationEmailTestIT {

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AmazonSQS sqsClient;

  @Autowired
  private IdentityProvider identityProvider;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  private final String VALID_EMAIL = "jaqucaz@dev.co.uk";
  private final String VALID_PASSWORD = "Password.123";
  private final String VALID_VERIFICATION_URL = "http://example.com";
  private final String VALID_ACCOUNT_NAME = "Funky Pigeotto";

  @BeforeEach
  public void startMockServer() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
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
  public void successfulResendVerificationEmailToUserWithActiveUserCode() throws JSONException {
    UserCreationResponseDto response = createAccountAndUser();
    String accountId = response.getAccountId();
    String accountUserId = response.getAccountUserId();

    givenResendVerificationEmailJourney()
        .forAccountId(accountId)
        .forAccountUserId(accountUserId)
        .forVerificationUrl(VALID_VERIFICATION_URL)
        .whenRequestToResendVerificationEmailIsMade()
        .then()
        .verificationEmailWasSent()
        .oldVerificationCodeWasDiscarded();
  }

  private UserCreationResponseDto createAccountAndUser() {
    AccountCreationResponseDto accountCreationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(VALID_ACCOUNT_NAME)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    UserCreationResponseDto userCreationResponseDto = givenCreateAdminUserJourney()
        .andEmail(VALID_EMAIL)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .forAccountId(accountCreationResponseDto.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    return userCreationResponseDto;
  }

  private ResendVerificationEmailJourneyAssertion givenResendVerificationEmailJourney() {
    return new ResendVerificationEmailJourneyAssertion(objectMapper, sqsClient, emailSqsQueueName,
        jdbcTemplate);
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }
}
