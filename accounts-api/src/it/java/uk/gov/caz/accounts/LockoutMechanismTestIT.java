package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.annotation.FullyRunningIntegrationTest;
import uk.gov.caz.accounts.assertion.CreateAccountJourneyAssertion;
import uk.gov.caz.accounts.assertion.CreateAdminUserJourneyAssertion;
import uk.gov.caz.accounts.assertion.LockoutMechanismAssertion;
import uk.gov.caz.accounts.assertion.LoginUserJourneyAssertion;
import uk.gov.caz.accounts.assertion.PasswordResetJourneyAssertion;
import uk.gov.caz.accounts.assertion.SetPasswordJourneyAssertion;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
public class LockoutMechanismTestIT {

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

  @Autowired
  private IdentityProvider identityProvider;

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

  @BeforeEach
  @AfterEach
  public void clearTables() {
    clearAllTables();
  }

  @Nested
  class WhenUserProvidesInvalidCredentials {

    @Test
    void shouldNotLoginUserAndIncreaseFailedLogins() {
      String accountName = "my-account11";
      String email = "my-account11@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      createAccountAndUser(accountName, email, password, verificationUrl);

      String invalidPassword = password + "b";
      whenUserTryToLoginWithIncorrectPasswordForTheFirstTime()
          .forEmail(email)
          .andPassword(invalidPassword)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpUnauthorisedStatusCode();

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(1)
          .hasNoLockoutTime();
    }

    @Test
    void shouldSetLockoutTimeWhenUserTriedLoginForTheFifthTime() {
      String accountName = "my-account12";
      String email = "my-account12@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      createAccountAndUser(accountName, email, password, verificationUrl);
      mockAttributes(email, 4, null);

      String invalidPassword = password + "b";
      whenUserTryToLoginWithIncorrectPasswordForTheFifthTime()
          .forEmail(email)
          .andPassword(invalidPassword)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpUnauthorisedStatusCode();

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(5)
          .lockoutTimeIsNotNull();
    }

    @Test
    void accountShouldBeAlreadyLockedByLockoutTime() {
      String accountName = "my-account13";
      String email = "my-account13@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(15);
      createAccountAndUser(accountName, email, password, verificationUrl);
      mockAttributes(email, 5, lockoutTime);

      String invalidPassword = password + "b";
      whenUserTryToLoginWithIncorrectPasswordAndAccountIsLockedByLockoutTime()
          .forEmail(email)
          .andPassword(invalidPassword)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpUnauthorisedStatusCode();

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(5)
          .hasLockoutTime(lockoutTime);
    }

    @Test
    void shouldSetFailedLoginToOneAndRestartTimeoutWhenAccountWasLockedMoreThan30Minutes() {
      String accountName = "my-account14";
      String email = "my-account14@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(45);
      createAccountAndUser(accountName, email, password, verificationUrl);
      mockAttributes(email, 5, lockoutTime);

      String invalidPassword = password + "b";
      whenUserTryToLoginWithIncorrectPasswordAndAccountIsNotLockedByLockoutTime()
          .forEmail(email)
          .andPassword(invalidPassword)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpUnauthorisedStatusCode();

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(1)
          .hasNoLockoutTime();
    }
  }

  @Nested
  class WhenUserProvidesValidCredentials {

    @Test
    void shouldSetFailedLoginToZeroAndRestartTimeoutWhenAccountWasLockedMoreThan30Minutes() {
      String accountName = "my-account15";
      String email = "my-account15@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(45);
      createAccountAndUser(accountName, email, password, verificationUrl);
      mockAttributes(email, 5, lockoutTime);

      whenUserTryToLoginWithCorrectPasswordAndAccountIsNotLockedByLockoutTime()
          .forEmail(email)
          .andPassword(password)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpOkStatusCode()
          .accountUserIsUpdatedInDatabaseWithSignInTimeStamp()
          .withEmailEqualTo(email);

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(0)
          .hasNoLockoutTime();
    }

    @Test
    void shouldNotUpdateAttributesWhenAccountWasLockedLessThan30Minutes() {
      String accountName = "my-account16";
      String email = "my-account16@my-mail.com";
      String password = "my-secret-password";
      String verificationUrl = "http://example.com";

      LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(25);
      createAccountAndUser(accountName, email, password, verificationUrl);
      mockAttributes(email, 5, lockoutTime);

      whenUserTryToLoginWithCorrectPasswordButAccountIsLockedByLockoutTime()
          .forEmail(email)
          .andPassword(password)
          .whenRequestToLogInIsMade()
          .then()
          .responseIsReturnedWithHttpUnauthorisedStatusCode()
          .accountUserIsNotUpdatedInDatabaseWithSignInTimeStamp();

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(5)
          .hasLockoutTime(lockoutTime);
    }
  }

  @Nested
  class WhenUserTryingToRestartPassword {

    @Test
    void shouldSetFailedLoginToZeroAndRestartTimeoutWhenThePasswordWasChanged()
        throws JSONException {
      String accountName = "my-account17";
      String email = "my-account17@my-mail.com";
      String oldPassword = "my-secret-password";
      String newPassword = "my-new-secret-password";
      String verificationUrl = "http://example.com";

      registerUserButDoNotVerifyEmail(email, oldPassword, accountName, verificationUrl);
      UUID validToken = generateValidToken(email);
      LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(45);
      mockAttributes(email, 5, lockoutTime);

      whenUserIsSettingNewPassword()
          .forToken(validToken)
          .forPassword(newPassword)
          .whenRequestToSetPassword()
          .then()
          .responseIsReturnedWithHttpNoContentStatusCode()
          .and()
          .userEmailHasBeenVerified(email);

      thenAfterLoginAttempt()
          .attributesForUser(email)
          .hasFailedLogins(0)
          .hasNoLockoutTime();
    }
  }

  private void mockAttributes(String email, int failedLogins, LocalDateTime lockoutTime) {
    mockAttributes()
        .mockFailedLoginsAndLockoutTime(email, failedLogins, lockoutTime);
  }

  private SetPasswordJourneyAssertion whenUserIsSettingNewPassword() {
    return givenSetPasswordJourney();
  }

  private UUID generateValidToken(String email) throws JSONException {
    return givenPasswordResetJourney()
        .forEmail(email)
        .whenRequestToResetPasswordMade()
        .then()
        .responseIsReturnedWithHttpNoContentStatusCode()
        .and()
        .resetUrlHasBeenSentToTheUser();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithCorrectPasswordButAccountIsLockedByLockoutTime() {
    return givenLoginUserJourney();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithCorrectPasswordAndAccountIsNotLockedByLockoutTime() {
    return givenLoginUserJourney();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithIncorrectPasswordAndAccountIsLockedByLockoutTime() {
    return whenUserTryToLoginWithIncorrectPassword();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithIncorrectPasswordAndAccountIsNotLockedByLockoutTime() {
    return whenUserTryToLoginWithIncorrectPassword();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithIncorrectPasswordForTheFirstTime() {
    return whenUserTryToLoginWithIncorrectPassword();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithIncorrectPasswordForTheFifthTime() {
    return whenUserTryToLoginWithIncorrectPassword();
  }

  private LoginUserJourneyAssertion whenUserTryToLoginWithIncorrectPassword() {
    return givenLoginUserJourney();
  }

  private void createAccountAndUser(String accountName, String email, String password,
      String verificationUrl) {
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
        .accountUserIsCreatedInDatabaseWithInsertTimeStamp()
        .accountUserIsCreatedInDatabaseWithEmptySignInTimeStamp()
        .adminUserIsCreatedInDatabase();
  }

  public void registerUserButDoNotVerifyEmail(String email, String password, String accountName,
      String verificationUrl) {
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

  private LoginUserJourneyAssertion givenLoginUserJourney() {
    return new LoginUserJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private LockoutMechanismAssertion mockAttributes() {
    return new LockoutMechanismAssertion((StubbedIdentityProvider) identityProvider);
  }

  private LockoutMechanismAssertion thenAfterLoginAttempt() {
    return new LockoutMechanismAssertion((StubbedIdentityProvider) identityProvider);
  }

  private SetPasswordJourneyAssertion givenSetPasswordJourney() {
    return new SetPasswordJourneyAssertion(objectMapper, identityProvider);
  }

  private PasswordResetJourneyAssertion givenPasswordResetJourney() {
    return new PasswordResetJourneyAssertion(objectMapper, sqsClient, emailSqsQueueName);
  }

  private void clearAllTables() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate,
        "caz_account.t_account_user_permission",
        "caz_account.t_account_user_code",
        "caz_account.t_account_user",
        "caz_account.t_account"
    );
  }
}