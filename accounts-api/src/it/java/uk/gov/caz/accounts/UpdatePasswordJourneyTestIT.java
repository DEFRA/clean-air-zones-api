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
import uk.gov.caz.accounts.assertion.UpdatePasswordJourneyAssertion;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;
import uk.gov.caz.accounts.repository.IdentityProvider;

@FullyRunningIntegrationTest
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-user-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class UpdatePasswordJourneyTestIT {

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

  private String email = RandomStringUtils.randomAlphabetic(10);
  private String password = RandomStringUtils.randomAlphabetic(10);

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
  public void canChangeHisPassword() {
    UserCreationResponseDto userCreation = registerUserAccount(email, password);

    givenUpdatePasswordJourney(userCreation)
        .forOldPassword(password)
        .forNewPassword(password+"z")
        .whenRequestToUpdatePasswordIsMade()
        .thenNoContentResponseIsReceived();
  }

  @Test
  public void returns500whenProvidedUserIdDoesNotExists() {
    UserCreationResponseDto userCreation = registerUserAccount(email, password);

    givenUpdatePasswordJourney(userCreation)
        .forOldPassword(password)
        .forNewPassword(password)
        .forAccountUserId(UUID.randomUUID())
        .whenRequestToUpdatePasswordIsMade()
        .then500ResponseIsReturned();
  }

  @Test
  public void returns422whenOldPasswordIsInvalid() {
    UserCreationResponseDto userCreation = registerUserAccount(email, password);

    givenUpdatePasswordJourney(userCreation)
        .forNewPassword(password)
        .forOldPassword(password+"Z")
        .whenRequestToUpdatePasswordIsMade()
        .then422ResponseIsReturned()
        .andErrorCodeEqualTo("oldPasswordInvalid")
        .andMessageEqualTo("The password you entered is incorrect");
  }

  @Test
  public void returns422whenRecentlyUsed() {
    UserCreationResponseDto userCreation = registerUserAccount(email, password);

    givenUpdatePasswordJourney(userCreation)
        .forNewPassword(password)
        .forOldPassword(password)
        .whenRequestToUpdatePasswordIsMade()
        .then422ResponseIsReturned()
        .andErrorCodeEqualTo("newPasswordReuse")
        .andMessageEqualTo("You have already used that password, choose a new one");
  }

  private UserCreationResponseDto registerUserAccount(String email, String password) {
    String accountName = "my-account";
    String verificationUrl = "http://example.com";

    AccountCreationResponseDto accountCreation = givenCreateAccountJourney()
        .forAccountWithName(accountName)
        .whenRequestToCreateAccountIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    UserCreationResponseDto userCreation = givenCreateAdminUserJourney()
        .andEmail(email)
        .andPassword(password)
        .andVerificationUrl(verificationUrl)
        .forAccountId(accountCreation.getAccountId())
        .whenRequestToCreateAdminUserIsMade()
        .then()
        .responseIsReturnedWithHttpCreatedStatusCode()
        .getCreationResponseDto();

    return userCreation;
  }


  private CreateAccountJourneyAssertion givenCreateAccountJourney() {
    return new CreateAccountJourneyAssertion(objectMapper, jdbcTemplate);
  }


  private CreateAdminUserJourneyAssertion givenCreateAdminUserJourney() {
    return new CreateAdminUserJourneyAssertion(objectMapper, jdbcTemplate, sqsClient,
        emailSqsQueueName);
  }

  private UpdatePasswordJourneyAssertion givenUpdatePasswordJourney(UserCreationResponseDto
      userCreationResponseDto) {
    return new UpdatePasswordJourneyAssertion(objectMapper, identityProvider,
        UUID.fromString(userCreationResponseDto.getAccountUserId()));
  }

}
