package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
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
import uk.gov.caz.accounts.assertion.ChangeUserEmailJourneyAssertion;
import uk.gov.caz.accounts.assertion.ConfirmEmailChangeJourneyAssertion;
import uk.gov.caz.accounts.assertion.CreateAccountJourneyAssertion;
import uk.gov.caz.accounts.assertion.CreateAdminUserJourneyAssertion;
import uk.gov.caz.accounts.assertion.VerifyUserAccountJourneyAssertion;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class EmailChangeTestIT {

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

  @Value("${services.sqs.email-change-template-id}")
  String emailChangeTemplateId;

  @Test
  public void successfullyInitiateUserEmailChange() {
    String newEmail = "my-updated-email@com.com";
    UserAndAccountDataHolder response = createAdminUserForAccount("a@b.com", "account-1");
    UUID accountUserId = response.getUserId();

    givenEmailChangeJourney()
        .andAccountUserId(accountUserId)
        .andNewEmail(newEmail)
        .whenRequestToChangeEmailIsMade()
        .responseIsReturnedWithEmptyPayloadAndHttpOkStatusCode()
        .emailChangeHasBeenSentToTheUser()
        .emailVerificationUserCodeHasBeenCreatedInDatabase()
        .userWithNewEmailIsCreatedInIdentityProvider()
        .pendingUserIdHasBeenSetForExistingUser();

    // when user wants to resend email
    givenEmailChangeJourney()
        .andAccountUserId(accountUserId)
        .andNewEmail(newEmail)
        .whenRequestToChangeEmailIsMade()
        .responseIsReturnedWithEmptyPayloadAndHttpOkStatusCode()
        .emailChangeHasBeenSentToTheUser();

    String anotherNewEmail = "my-updated-email-1@com.com";

    // initiate email change when a change is in progress
    String changeEmailToken = givenEmailChangeJourney()
        .andAccountUserId(accountUserId)
        .andNewEmail(anotherNewEmail)
        .whenRequestToChangeEmailIsMade()
        .responseIsReturnedWithEmptyPayloadAndHttpOkStatusCode()
        .emailChangeHasBeenSentToTheUser()
        .emailVerificationUserCodeHasBeenCreatedInDatabase()
        .previousVerificationUserCodeHasBeenDiscarded()
        .userWithNewEmailIsCreatedInIdentityProvider()
        .userWithEmailHasBeenRemovedFromIdentityProvider(newEmail)
        .pendingUserIdHasBeenSetForExistingUser()
        .getChangeEmailToken();

    // complete the email change process
    givenConfirmEmailChangeJourney()
        .forToken(UUID.fromString(changeEmailToken))
        .andPassword("p4ssw9rd.12.12")
        .whenRequestToConfirmEmailIsMade()
        .then()
        .responseIsReturnedWithOkStatusAndEmail(anotherNewEmail)
        .emailVerificationUserCodeHasBeenUsed()
        .userWithEmailHasBeenRemovedFromIdentityProvider("a@b.com")
        .identityProviderUserHasBeenReassigned();
  }

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

  private ConfirmEmailChangeJourneyAssertion givenConfirmEmailChangeJourney() {
    return new ConfirmEmailChangeJourneyAssertion(objectMapper, jdbcTemplate, identityProvider);
  }

  private ChangeUserEmailJourneyAssertion givenEmailChangeJourney() {
    return new ChangeUserEmailJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName, identityProvider, emailChangeTemplateId);
  }

  private UserAndAccountDataHolder createAdminUserForAccount(String email, String accountName) {
    String accountId = createAccount(accountName);
    CreateAdminUserJourneyAssertion createAdminUserJourneyAssertion = givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword("Password.123")
        .andVerificationUrl("http://example.com")
        .forAccountId(accountId)
        .whenRequestToCreateAdminUserIsMade()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userEmailVerificationUrlHasBeenSentToTheUser();

    String verificationToken = createAdminUserJourneyAssertion
        .getVerificationToken();

    givenVerifyUserAccountJourney()
        .forVerificationToken(UUID.fromString(verificationToken))
        .whenRequestToVerifyAccountByTokenIsMade()
        .then()
        .responseIsReturnedWithHttpOkStatusCode();

    return new UserAndAccountDataHolder(
        UUID.fromString(accountId),
        accountName,
        UUID.fromString(createAdminUserJourneyAssertion.getCreationResponseDto().getAccountUserId())
    );
  }

  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }

  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private VerifyUserAccountJourneyAssertion givenVerifyUserAccountJourney() {
    return new VerifyUserAccountJourneyAssertion(objectMapper);
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

  @lombok.Value
  private static class UserAndAccountDataHolder {

    UUID accountId;
    String accountName;
    UUID userId;
  }
}
