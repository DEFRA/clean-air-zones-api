package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.util.Collections;
import java.util.UUID;
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
import uk.gov.caz.accounts.assertion.PasswordResetJourneyAssertion;
import uk.gov.caz.accounts.assertion.SetPasswordJourneyAssertion;
import uk.gov.caz.accounts.assertion.ValidateTokenJourneyAssertion;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class PasswordResetJourneyTestIT {

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
  public void resetPasswordAndLogInIntoAccountTest() throws JSONException {
    UUID invalidToken = UUID.randomUUID();
    String accountName = "resetpassword";
    String email = "test@resetpassword.com";
    String oldPassword = "my-secret-password";
    String newPassword = "my-new-secret-password";
    String verificationUrl = "http://example.com";

    registerUserButDoNotVerifyEmail(email, oldPassword, accountName, verificationUrl);

    // User request password reset
    UUID validToken = givenPasswordResetJourney()
        .forEmail(email)
        .whenRequestToResetPasswordMade()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .resetUrlHasBeenSentToTheUser();

    // Then UI validates if provided token from email is valid
    givenValidateTokenJourney()
        .forToken(validToken)
        .whenRequestToValidateToken()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode();

    // Returns bad request if token is invalid
    givenValidateTokenJourney()
        .forToken(invalidToken)
        .whenRequestToValidateToken()
        .then()
        .responseIsReturnedBadRequestStatusCode();

    // Then User sets new password with provided valid token
    givenSetPasswordJourney()
        .forToken(validToken)
        .forPassword(newPassword)
        .whenRequestToSetPassword()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .userEmailHasBeenVerified(email);

    // Then User tries to set new password with provided USED token
    givenSetPasswordJourney()
        .forToken(validToken)
        .forPassword(newPassword)
        .whenRequestToSetPassword()
        .then()
        .responseIsReturnedBadRequestStatusCode();

    // Then User can login with new password.
    givenLoginUserJourney()
        .forEmail(email)
        .andPassword(newPassword)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode();

    // Then User cannot login with old password.
    givenLoginUserJourney()
        .forEmail(email)
        .andPassword(oldPassword)
        .whenRequestToLogInIsMade()
        .then()
        .responseIsReturnedWithHttpUnauthorisedStatusCode();

    // And token is invalid when used.
    givenValidateTokenJourney()
        .forToken(validToken)
        .whenRequestToValidateToken()
        .then()
        .responseIsReturnedBadRequestStatusCode();
  }

  @Test
  public void resetPasswordThrowsRecentlyUsedExceptionWhenItsUsed() throws JSONException {
    String accountName = "resetpassword2";
    String email = "test2@resetpassword.com";
    String oldPassword = "my-secret-password";
    String newPassword = "my-new-secret-password";
    String verificationUrl = "http://example.com";

    registerUserButDoNotVerifyEmail(email, oldPassword, accountName, verificationUrl);

    // User request password reset
    UUID validToken = givenPasswordResetJourney()
        .forEmail(email)
        .whenRequestToResetPasswordMade()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .resetUrlHasBeenSentToTheUser();

    // Then UI validates if provided token from email is valid
    givenValidateTokenJourney()
        .forToken(validToken)
        .whenRequestToValidateToken()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode();

    // Then User sets new password with provided valid token
    givenSetPasswordJourney()
        .forToken(validToken)
        .forPassword(newPassword)
        .whenRequestToSetPassword()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .userEmailHasBeenVerified(email);

    // User request password reset once again
    UUID validToken2 = givenPasswordResetJourney()
        .forEmail(email)
        .whenRequestToResetPasswordMade()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .resetUrlHasBeenSentToTheUser();

    // Then UI validates if provided token from email is valid
    givenValidateTokenJourney()
        .forToken(validToken2)
        .whenRequestToValidateToken()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode();

    // Then User tries to set a new password but it was already used
    givenSetPasswordJourney()
        .forToken(validToken2)
        .forPassword(newPassword)
        .whenRequestToSetPassword()
        .then()
        .responseIs422WithMessage("You have already used that password, choose a new one")
        .and()
        .userEmailHasBeenVerified(email);

  }

  public void registerUserButDoNotVerifyEmail(String email, String password, String accountName,
      String verificationUrl) throws JSONException {
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
        .userEmailVerificationUrlHasBeenSentToTheUser();
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private PasswordResetJourneyAssertion givenPasswordResetJourney() {
    return new PasswordResetJourneyAssertion(objectMapper, sqsClient, emailSqsQueueName);
  }

  private ValidateTokenJourneyAssertion givenValidateTokenJourney() {
    return new ValidateTokenJourneyAssertion(objectMapper);
  }

  private SetPasswordJourneyAssertion givenSetPasswordJourney() {
    return new SetPasswordJourneyAssertion(objectMapper, identityProvider);
  }

  private LoginUserJourneyAssertion givenLoginUserJourney() {
    return new LoginUserJourneyAssertion(objectMapper, jdbcTemplate);
  }
}
