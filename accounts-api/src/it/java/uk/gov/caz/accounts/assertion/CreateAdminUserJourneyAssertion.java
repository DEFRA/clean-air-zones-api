package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class CreateAdminUserJourneyAssertion {

  private static final String ACCOUNT_TABLE = "caz_account.t_account";
  private static final String ACCOUNT_USER_TABLE = "caz_account.t_account_user";
  private static final String ACCOUNT_USER_CODE_TABLE = "caz_account.t_account_user_code";
  private static final String ACCOUNT_USER_PERMISSION_TABLE =
      "caz_account.t_account_user_permission";

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final AmazonSQS sqsClient;
  private final String emailSqsQueueName;

  private UserCreationResponseDto creationResponseDto;

  // create-user parameters
  private String email;
  private String password;
  private String verificationUrl;

  private Response createAdminResponse;
  private String correlationId;
  private String accountId;
  private String verificationToken;

  public CreateAdminUserJourneyAssertion andEmail(String email) {
    this.email = email;
    return this;
  }

  public CreateAdminUserJourneyAssertion andPassword(String password) {
    this.password = password;
    return this;
  }

  public CreateAdminUserJourneyAssertion andVerificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
    return this;
  }

  public CreateAdminUserJourneyAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public CreateAdminUserJourneyAssertion whenRequestToCreateAdminUserIsMade() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
    this.correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

    this.createAdminResponse = RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(toJsonString(createAccountCreationRequestDto()))
        .when()
        .post("/{accountId}/users", accountId);
    return this;
  }

  public CreateAdminUserJourneyAssertion then() {
    return this;
  }

  public CreateAdminUserJourneyAssertion responseIsReturnedWithHttpCreatedStatusCode() {
    this.creationResponseDto = createAdminResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .as(UserCreationResponseDto.class);
    return this;
  }

  public CreateAdminUserJourneyAssertion responseIsReturnedWithHttpUnprocessableEntityStatusCode() {
    createAdminResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    return this;
  }

  public CreateAdminUserJourneyAssertion responseIsReturnedWithHttpNotFoundCode() {
    createAdminResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NOT_FOUND.value());
    return this;

  }

  public CreateAdminUserJourneyAssertion andMessageEqualTo(String message) {
    createAdminResponse.then().body("message", equalTo(message));
    return this;
  }

  public CreateAdminUserJourneyAssertion andErrorCodeEqualTo(String errorCode) {
    createAdminResponse.then().body("errorCode", equalTo(errorCode));
    return this;
  }

  public CreateAdminUserJourneyAssertion userEmailVerificationUrlHasBeenSentToTheUser() {
    return verificationUrlHasBeenSentToTheUser("link");
  }

  @SneakyThrows
  private CreateAdminUserJourneyAssertion verificationUrlHasBeenSentToTheUser(
      String verificationTokenPayloadKey) {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).isNotEmpty();
    String sendEmailBody = messages.iterator().next().getBody();
    JSONObject jsonEmailBody = new JSONObject(sendEmailBody);
    JSONObject jsonEmailVariables = new JSONObject(jsonEmailBody.get("personalisation").toString());
    String verificationUrl = jsonEmailVariables.get(verificationTokenPayloadKey).toString();
    String verificationToken = verificationUrl.split("token=")[1];
    assertThat(verificationToken).isNotBlank();

    this.verificationToken = verificationToken;

    return this;
  }

  public String getVerificationToken() {
    return verificationToken;
  }

  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult = sqsClient
        .receiveMessage(queueUrlResult.getQueueUrl());
    List<Message> messages = receiveMessageResult.getMessages();
    for (Message message : messages) {
      sqsClient.deleteMessage(queueUrlResult.getQueueUrl(), message.getReceiptHandle());
    }
    return messages;
  }

  public UserCreationResponseDto adminUserIsCreatedInDatabase() {
    verifyThatSingleUserDataAreStoredInDatabase();

    return this.creationResponseDto;
  }

  public UserCreationResponseDto getCreationResponseDto() {
    return creationResponseDto;
  }

  private UserForAccountCreationRequestDto createAccountCreationRequestDto() {
    return UserForAccountCreationRequestDto.builder()
        .email(email)
        .password(password)
        .verificationUrl(verificationUrl)
        .build();
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }

  private void verifyThatSingleUserDataAreStoredInDatabase() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_TABLE,
        "account_id = '" + accountId + "'");
    int accountUserRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "is_owner = TRUE");

    assertThat(accountRowsCount).isEqualTo(1);
    assertThat(accountUserRowsCount).isEqualTo(1);
  }

  public CreateAdminUserJourneyAssertion accountUserIsCreatedInDatabaseWithInsertTimeStamp() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "INSERT_TIMESTMP IS NOT NULL AND INSERT_TIMESTMP > now() - INTERVAL '1 MINUTE'");

    assertThat(accountRowsCount).isEqualTo(1);
    return this;
  }

  public CreateAdminUserJourneyAssertion accountUserIsCreatedInDatabaseWithEmptySignInTimeStamp() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "LAST_SIGN_IN_TIMESTMP IS NULL");

    assertThat(accountRowsCount).isEqualTo(1);
    return this;
  }

  public CreateAdminUserJourneyAssertion adminUserIsNotCreatedInDatabase() {
    int accountUserRowsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, ACCOUNT_USER_TABLE);
    assertThat(accountUserRowsCount).isZero();
    return this;
  }

  public CreateAdminUserJourneyAssertion verificationAccountUserCodeIsCreatedInTheDatabase() {
    int verificationCodesCount = JdbcTestUtils
        .countRowsInTableWhere(jdbcTemplate, ACCOUNT_USER_CODE_TABLE,
            "code_type = 'USER_VERIFICATION'");
    assertThat(verificationCodesCount).isEqualTo(1);
    return this;
  }

  public CreateAdminUserJourneyAssertion userIsCreatedWithThisPermissionsCount(int count) {
    String userId = this.creationResponseDto.getAccountUserId();
    int permissionRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_PERMISSION_TABLE,
        String.format("account_user_id = '%s'", userId));

    assertThat(permissionRowsCount).isEqualTo(count);
    return this;
  }
}
