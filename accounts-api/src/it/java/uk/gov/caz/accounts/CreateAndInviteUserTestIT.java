package uk.gov.caz.accounts;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
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
import uk.gov.caz.accounts.assertion.CreateAndInviteUserJourneyAssertion;
import uk.gov.caz.accounts.assertion.VerifyUserAccountJourneyAssertion;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.model.Permission;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CreateAndInviteUserTestIT {

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
  public void createAndInviteUser() {
    String invitedUserEmail = "invited-user@a.gov.uk";
    String invitedUserName = "invitedUser";
    String password = "p4$$word";
    String newPassword = password + "z";
    UserAndAccountDataHolder data = createAccountUserWithRandomAccountName();

    givenCreateAndInviteUserJourney()
        .forAccountId(data.getAccountId())
        .forAccountName(data.getAccountName())
        .forEmail(invitedUserEmail)
        .forUserName(invitedUserName)
        .forPassword(password)
        .forUserSendingInvitation(data.getUserId())

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userIsCreated()
        .accountUserCodeHasBeenCreated()
        .invitationEmailHasBeenSent()

        // user has been created, but has not been verified

        .whenRequestToLogInIsMadeUsingWrongPassword()
        .then()
        .responseForLogInRequestIsReturnedWithUnauthorisedStatusCode()

        .whenRequestToLogInIsMadeUsingCorrectPassword()
        .then()
        .responseForLogInRequestIsReturnedWithUnauthorisedStatusCode()

        .whenRequestToSetPasswordMade(newPassword)
        .then()
        .responseForSetPasswordIsReturnedWithHttpNoContent()
        .accountUserCodeHasBeenMarkedAsUsed()

        // user has been verified, now we should be able to log in

        .whenRequestToLogInIsMadeUsingCorrectPassword()
        .then()
        .responseForLogInRequestIsReturnedWithOkStatusCode();
  }

  @Test
  public void accountMultipayerAttributeIsTrueWhenUserHasMakePaymentsPermission() {
    String invitedUserEmail = "invited-user-2@a.gov.uk";
    String invitedUserName = "invitedUser2";
    String password = "p4$$word";
    UserAndAccountDataHolder data = createAccountUserWithRandomAccountName();

    givenCreateAndInviteUserJourney()
        .forAccountId(data.getAccountId())
        .forAccountName(data.getAccountName())
        .forEmail(invitedUserEmail)
        .forUserName(invitedUserName)
        .forPassword(password)
        .forUserSendingInvitation(data.getUserId())
        .forPermissions(Sets.newHashSet(Permission.MAKE_PAYMENTS))

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userIsCreated()
        .accountIsMultipayer(true);
  }

  @Test
  public void accountMultipayerAttributeIsFalseWhenMakePaymentsPermissionIsMissingForUser() {
    String invitedUserEmail = "invited-user-3@a.gov.uk";
    String invitedUserName = "invitedUser3";
    String password = "p4$$word";
    UserAndAccountDataHolder data = createAccountUserWithRandomAccountName();

    givenCreateAndInviteUserJourney()
        .forAccountId(data.getAccountId())
        .forAccountName(data.getAccountName())
        .forEmail(invitedUserEmail)
        .forUserName(invitedUserName)
        .forPassword(password)
        .forUserSendingInvitation(data.getUserId())
        .forPermissions(Sets.newHashSet(Permission.MANAGE_MANDATES, Permission.MANAGE_USERS))

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .userIsCreated()
        .accountIsMultipayer(false);
  }

  @Test
  public void return422ErrorWhenTryingToInviteExistingUser() {
    String invitedUserEmail = "invited-user-1@a.gov.uk";
    String invitedUserName = "invitedUser1";
    String anyPassword = "p4$$word1";
    UserAndAccountDataHolder firstAdminUserDataHolder = createAccountUserWithRandomAccountName();
    UserAndAccountDataHolder secondAdminUserDataHolder = createAccountUserWithRandomAccountName();

    givenCreateAndInviteUserJourney()
        .forAccountId(firstAdminUserDataHolder.getAccountId())
        .forAccountName(firstAdminUserDataHolder.getAccountName())
        .forUserName(invitedUserName)
        .forEmail(invitedUserEmail)
        .forPassword(anyPassword)
        .forUserSendingInvitation(firstAdminUserDataHolder.getUserId())

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode();

    // standard user with 'invitedUserEmail' has been created, but it's not been verified
    // try to invite the same user for two different accounts

    givenCreateAndInviteUserJourney()
        .forAccountId(firstAdminUserDataHolder.getAccountId())
        .forAccountName(firstAdminUserDataHolder.getAccountName())
        .forUserName(invitedUserName)
        .forEmail(invitedUserEmail)
        .forPassword(anyPassword)
        .forUserSendingInvitation(firstAdminUserDataHolder.getUserId())

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode();

    givenCreateAndInviteUserJourney()
        .forAccountId(secondAdminUserDataHolder.getAccountId())
        .forAccountName(secondAdminUserDataHolder.getAccountName())
        .forUserName(invitedUserName)
        .forEmail(invitedUserEmail)
        .forPassword(anyPassword)
        .forUserSendingInvitation(secondAdminUserDataHolder.getUserId())

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpUnprocessableEntityStatusCode();
  }

  @Test
  public void return404ErrorWhenTryingToInviteUserForNonExistingAccount() {
    UUID nonExistingAccountId = UUID.randomUUID();
    UUID nonExistingAccountUserId = UUID.randomUUID();
    String anyAccountName = "any-account";
    String invitedUserEmail = "invited-user-1@a.gov.uk";
    String invitedUserName = "invitedUser1";
    String anyPassword = "p4$$word1";

    givenCreateAndInviteUserJourney()
        .forAccountId(nonExistingAccountId)
        .forAccountName(anyAccountName)
        .forUserName(invitedUserName)
        .forEmail(invitedUserEmail)
        .forPassword(anyPassword)
        .forUserSendingInvitation(nonExistingAccountUserId)

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpNotFoundStatusCode();
  }

  @Test
  public void return404ErrorWhenTryingToInviteUserForNonExistingUser() {
    UUID nonExistingAccountUserId = UUID.randomUUID();
    String accountName = "my-account";
    String invitedUserEmail = "invited-user-1@a.gov.uk";
    String invitedUserName = "invitedUser1";
    String anyPassword = "p4$$word1";

    String accountId = createAccount(accountName);

    givenCreateAndInviteUserJourney()
        .forAccountId(UUID.fromString(accountId))
        .forAccountName(accountName)
        .forUserName(invitedUserName)
        .forEmail(invitedUserEmail)
        .forPassword(anyPassword)
        .forUserSendingInvitation(nonExistingAccountUserId)

        .whenRequestToCreateAndInviteUserIsMade()
        .then()
        .responseIsReturnedWithHttpNotFoundStatusCode();
  }

  private CreateAndInviteUserJourneyAssertion givenCreateAndInviteUserJourney() {
    return new CreateAndInviteUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
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

  private UserAndAccountDataHolder createAccountUserWithRandomAccountName() {
    String VALID_ACCOUNT_NAME = "jaqu";
    return createAccountAndUserWithAccountNameAndEmail(
        (VALID_ACCOUNT_NAME + RandomStringUtils.randomAlphabetic(2)).toLowerCase(),
        validEmail()
    );
  }

  private UserAndAccountDataHolder createAccountAndUserWithAccountNameAndEmail(String accountName, String email) {
    String accountId = createAccount(accountName);
    return createAdminUserForAccount(email, accountName, accountId);
  }

  private String createAccount(String accountName) {
    return givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto()
        .getAccountId();
  }

  private UserAndAccountDataHolder createAdminUserForAccount(String email, String accountName,
      String accountId) {
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

    givenVerifyNonExistingAdminUserJourney()
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

  @lombok.Value
  private static class UserAndAccountDataHolder {
    UUID accountId;
    String accountName;
    UUID userId;
  }
}
