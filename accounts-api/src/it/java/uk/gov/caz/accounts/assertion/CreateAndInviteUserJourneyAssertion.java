package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.internal.Uris;
import org.assertj.core.util.Sets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.controller.LoginController;
import uk.gov.caz.accounts.controller.PasswordController;
import uk.gov.caz.accounts.dto.CreateAndInviteUserRequestDto;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.dto.SetPasswordRequest;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class CreateAndInviteUserJourneyAssertion {

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final AmazonSQS sqsClient;
  private final String emailSqsQueueName;

  private UUID accountId;
  private UUID invitingUserAccountUserId;
  private String email;
  private String name;
  private String accountName;
  private String currentPassword;

  private String correlationId;
  private Response createAndInviteUserResponse;
  private Response setUserPasswordResponse;
  private Response logInResponse;
  private String verificationToken;
  private Set<Permission> permissions = Sets.newHashSet();

  public CreateAndInviteUserJourneyAssertion forAccountId(UUID accountId) {
    this.accountId = accountId;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forAccountName(String accountName) {
    this.accountName = accountName;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forUserSendingInvitation(UUID userId) {
    this.invitingUserAccountUserId = userId;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forEmail(String email) {
    this.email = email;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forUserName(String name) {
    this.name = name;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forPassword(String password) {
    this.currentPassword = password;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion forPermissions(Set<Permission> permissions) {
    this.permissions = permissions;
    return this;
  }

  public CreateAndInviteUserJourneyAssertion then() {
    return this;
  }

  @SneakyThrows
  public CreateAndInviteUserJourneyAssertion whenRequestToCreateAndInviteUserIsMade() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
    this.correlationId = "f11281a2-306b-11ea-b08d-737c67689306";

    CreateAndInviteUserRequestDto request = CreateAndInviteUserRequestDto.builder()
        .email(email)
        .isAdministeredBy(invitingUserAccountUserId.toString())
        .name(name)
        .permissions(permissions.stream().map(Permission::name).collect(Collectors.toSet()))
        .verificationUrl("http://localhost")
        .build();

    this.createAndInviteUserResponse = RestAssured.given()
        .body(objectMapper.writeValueAsString(request))
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post("{accountId}/user-invitations", accountId);
    return this;
  }

  @SneakyThrows
  public CreateAndInviteUserJourneyAssertion whenRequestToSetPasswordMade(String password) {
    RestAssured.basePath = PasswordController.BASE_PATH;

    SetPasswordRequest request = SetPasswordRequest.builder()
        .password(password)
        .token(UUID.fromString(verificationToken))
        .build();

    setUserPasswordResponse = RestAssured.given()
        .body(objectMapper.writeValueAsString(request))
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .put(PasswordController.SET_PATH);
    this.currentPassword = password;
    return this;
  }

  @SneakyThrows
  public CreateAndInviteUserJourneyAssertion whenRequestToLogInIsMadeUsingCorrectPassword() {
    return whenRequestToLogInIsMadeUsingPassword(currentPassword);
  }

  public CreateAndInviteUserJourneyAssertion whenRequestToLogInIsMadeUsingWrongPassword() {
    return whenRequestToLogInIsMadeUsingPassword("wrong password");
  }

  @SneakyThrows
  public CreateAndInviteUserJourneyAssertion whenRequestToLogInIsMadeUsingPassword(String password) {
    RestAssured.basePath = LoginController.LOGIN_PATH;

    LoginRequestDto request = LoginRequestDto.builder()
        .email(email)
        .password(password)
        .build();

    logInResponse = RestAssured.given()
        .body(objectMapper.writeValueAsString(request))
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post();
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseForLogInRequestIsReturnedWithUnauthorisedStatusCode() {
    logInResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseForLogInRequestIsReturnedWithOkStatusCode() {
    logInResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    createAndInviteUserResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseIsReturnedWithHttpCreatedStatusCode() {
    createAndInviteUserResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.CREATED.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseIsReturnedWithHttpUnprocessableEntityStatusCode() {
    createAndInviteUserResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion responseIsReturnedWithHttpNotFoundStatusCode() {
    createAndInviteUserResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NOT_FOUND.value());
    return this;
  }

  public CreateAndInviteUserJourneyAssertion andMessageIsEqualTo(String message) {
    createAndInviteUserResponse.then()
        .body("message", equalTo(message));
    return this;
  }

  public CreateAndInviteUserJourneyAssertion andErrorCodeIsEqualTo(String errorCode) {
    createAndInviteUserResponse.then()
        .body("errorCode", equalTo(errorCode));
    return this;
  }

  public CreateAndInviteUserJourneyAssertion userIsCreated() {
    int usersByInvitingUserIdCount = JdbcTestUtils.countRowsInTableWhere(
        jdbcTemplate,
        "caz_account.t_account_user",
        "account_id IS NOT NULL "
            + "AND user_id IS NOT NULL "
            + "AND is_owner IS FALSE "
            + "AND is_administrated_by = '" + invitingUserAccountUserId + "'"
    );
    assertThat(usersByInvitingUserIdCount).isOne();
    return this;
  }

  public CreateAndInviteUserJourneyAssertion accountUserCodeHasBeenCreated() {
    int accountUserCodeCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account_user_code",
        "status = 'ACTIVE' "
            + "AND code_type = 'PASSWORD_RESET' "
    );
    assertThat(accountUserCodeCount).isOne();
    return this;
  }

  public CreateAndInviteUserJourneyAssertion accountUserCodeHasBeenMarkedAsUsed() {
    int usedPasswordResetCodesCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account_user_code",
        "status = 'USED' "
            + "AND code_type = 'PASSWORD_RESET' "
    );

    int allPasswordResetCodesCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account_user_code",
        "code_type = 'PASSWORD_RESET'"
    );

    assertThat(allPasswordResetCodesCount).isEqualTo(usedPasswordResetCodesCount);
    return this;
  }

  public CreateAndInviteUserJourneyAssertion accountIsMultipayer(boolean expected) {
    String multiPayerText = expected ? "TRUE" : "FALSE";
    int rowCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        "caz_account.t_account",
        String.format("multi_payer_account = %s "
            + "AND account_id = '%s'", multiPayerText, accountId)
    );
    assertThat(rowCount).isEqualTo(1);
    return this;
  }

  @SneakyThrows
  public CreateAndInviteUserJourneyAssertion invitationEmailHasBeenSent() {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).hasSize(1);
    Map<String, String> payload = readToMap(messages.iterator().next().getBody());

    assertThat(payload).containsOnlyKeys("templateId", "emailAddress", "personalisation",
        "reference");
    assertThat(payload).containsEntry("emailAddress", email);

    Map<String, String> personalisation = readToMap(payload.get("personalisation"));
    assertThat(personalisation).containsEntry("organisation", accountName);

    URI setPasswordLink = URI.create(personalisation.get("link"));
    assertThat(setPasswordLink).hasParameter("token");

    assertThat(setPasswordLink).hasParameter("account", accountId.toString());
    verificationToken = extractTokenFromPasswordResetLink(setPasswordLink);
    return this;
  }

  private String extractTokenFromPasswordResetLink(URI setPasswordLink) {
    return Uris.getParameters(setPasswordLink.getQuery()).get("token").iterator().next();
  }

  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult =
        sqsClient.receiveMessage(queueUrlResult.getQueueUrl());
    return receiveMessageResult.getMessages();
  }

  @SneakyThrows
  private Map<String, String> readToMap(String body) {
    return objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
  }

  public CreateAndInviteUserJourneyAssertion responseForSetPasswordIsReturnedWithHttpNoContent() {
    setUserPasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NO_CONTENT.value());
    return this;
  }
}
