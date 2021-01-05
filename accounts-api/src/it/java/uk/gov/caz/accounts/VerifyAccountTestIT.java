package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
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
import uk.gov.caz.accounts.assertion.VerifyUserAccountJourneyAssertion;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class VerifyAccountTestIT {

  private final String VALID_ACCOUNT_NAME = "jaqu";
  private final String VALID_PASSWORD = "Password.123";
  private final String VALID_VERIFICATION_URL = "http://example.com";

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
  public void return422ErrorWhenTryingToVerifyNonExistingUser() {
    givenVerifyNonExistingAdminUserJourney()
        .forVerificationToken(UUID.randomUUID())
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageIsEqualTo("Invalid token");
  }

  @Test
  public void verifyUserWithVerificationToken() {
    givenVerifyUserWithExistingAdminUserByTokenJourney()
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode();
  }

  @Test
  public void return422ErrorWhenTryingToVerifyUserWithWrongVerificationToken() {
    givenVerifyUserWithExistingAdminUserByRandomTokenJourney()
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageIsEqualTo("Invalid token")
        .andErrorCodeIsEqualTo("invalid");
  }

  @Test
  public void return422ErrorWhenTryingToVerifyUserWithExpiredVerificationToken() {
    givenVerifyUserWithExistingUserByExpiredTokenJourney()
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageIsEqualTo("Expired token")
        .andErrorCodeIsEqualTo("expired");
  }

  @Test
  public void return422ErrorWhenTryingToVerifyAlreadyVerifiedUser() {
    givenVerifyAlreadyVerifiedUserJourney()
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode()
        .andMessageIsEqualTo("Email already verified")
        .andErrorCodeIsEqualTo("emailAlreadyVerified");
  }

  private VerifyUserAccountJourneyAssertion givenVerifyNonExistingAdminUserJourney() {
    return givenVerifyUserAccountJourney();
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private VerifyUserAccountJourneyAssertion givenVerifyUserAccountJourney() {
    return new VerifyUserAccountJourneyAssertion(objectMapper);
  }

  private String validEmail() {
    return "jaqu" + RandomStringUtils.randomAlphabetic(3) + "@dev.co.uk";
  }

  private VerifyUserAccountJourneyAssertion givenVerifyUserWithExistingUserByExpiredTokenJourney() {
    String token = createAccountUserWithRandomAccountName();
    expireAllUserCodes();
    return givenVerifyNonExistingAdminUserJourney()
        .forVerificationToken(UUID.fromString(token));
  }

  private VerifyUserAccountJourneyAssertion givenVerifyAlreadyVerifiedUserJourney() {
    String email = validEmail();
    String token = createAccountAndUserWithAccountNameAndEmail(
        VALID_ACCOUNT_NAME + RandomStringUtils.randomAlphabetic(2),
        email
    );
    verifyUserByEmail(email);

    return givenVerifyNonExistingAdminUserJourney()
        .forVerificationToken(UUID.fromString(token));
  }

  private void verifyUserByEmail(String email) {
    identityProvider.verifyEmail(identityProvider.getUser(email));
  }

  private void expireAllUserCodes() {
    jdbcTemplate.update("update caz_account.t_account_user_code "
        + "set expiration = expiration - interval '7 days'");
  }

  private VerifyUserAccountJourneyAssertion givenVerifyUserWithExistingAdminUserByRandomTokenJourney() {
    return givenVerifyUserWithExistingAdminUserByTokenJourney()
        .forVerificationToken(UUID.randomUUID());
  }

  private VerifyUserAccountJourneyAssertion givenVerifyUserWithExistingAdminUserByTokenJourney() {
    String verificationToken = createAccountUserWithRandomAccountName();

    return givenVerifyNonExistingAdminUserJourney()
        .forVerificationToken(UUID.fromString(verificationToken));
  }

  private String createAccountUserWithRandomAccountName() {
    return createAccountAndUserWithAccountNameAndEmail(
        VALID_ACCOUNT_NAME + RandomStringUtils.randomAlphabetic(2),
        validEmail()
    );
  }

  private String createAccountAndUserWithAccountNameAndEmail(String accountName, String email) {
    AccountCreationResponseDto accountCreationResponseDto = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    return givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword(VALID_PASSWORD)
        .andVerificationUrl(VALID_VERIFICATION_URL)
        .forAccountId(accountCreationResponseDto.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userEmailVerificationUrlHasBeenSentToTheUser()
        .getVerificationToken();
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }
}
