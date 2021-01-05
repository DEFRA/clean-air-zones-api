package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.UpdateEmailController;
import uk.gov.caz.accounts.dto.InitiateEmailChangeRequest;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class ChangeUserEmailJourneyAssertion {

  private static final String ACCOUNT_USER_TABLE = "caz_account.t_account_user";
  private static final String ACCOUNT_USER_CODE_TABLE = "caz_account.t_account_user_code";

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final AmazonSQS sqsClient;
  private final String emailSqsQueueName;
  private final IdentityProvider identityProvider;
  private final String emailChangeTemplateId;

  // change-email parameters
  private String newEmail;

  private Response updateEmailResponse;
  private String correlationId;
  private UUID accountUserId;

  private String changeEmailToken;

  public ChangeUserEmailJourneyAssertion andNewEmail(String email) {
    this.newEmail = email;
    return this;
  }

  public ChangeUserEmailJourneyAssertion andAccountUserId(UUID accountUserId) {
    this.accountUserId = accountUserId;
    return this;
  }

  public ChangeUserEmailJourneyAssertion then() {
    return this;
  }

  public ChangeUserEmailJourneyAssertion whenRequestToChangeEmailIsMade() {
    RestAssured.basePath = UpdateEmailController.PATH + "/change-request";

    this.correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

    this.updateEmailResponse = RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(toJsonString(createUpdateEmailRequestDto()))
        .when()
        .put();

    return this;
  }

  public ChangeUserEmailJourneyAssertion responseIsReturnedWithEmptyPayloadAndHttpOkStatusCode() {
    updateEmailResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .statusCode(HttpStatus.OK.value())
        .noRootPath();

    return this;
  }

  @SneakyThrows
  public ChangeUserEmailJourneyAssertion emailChangeHasBeenSentToTheUser() {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).isNotEmpty();

    Map<String, String> payload = readToMap(messages.iterator().next().getBody());

    assertThat(payload).containsOnlyKeys("templateId", "emailAddress", "personalisation",
        "reference");
    assertThat(payload).containsEntry("emailAddress", newEmail);
    assertThat(payload).containsEntry("templateId", emailChangeTemplateId);

    Map<String, String> personalisation = readToMap(payload.get("personalisation"));

    URI setPasswordLink = URI.create(personalisation.get("link"));
    assertThat(setPasswordLink).hasParameter("token");
    storeChangeEmailToken(setPasswordLink);
    return this;
  }

  private void storeChangeEmailToken(URI setPasswordLink) {
    this.changeEmailToken = setPasswordLink.getQuery().split("=")[1];
  }

  public ChangeUserEmailJourneyAssertion emailVerificationUserCodeHasBeenCreatedInDatabase() {
    thereIsOneActiveEmailChangeVerificationUserCode();
    return this;
  }

  public ChangeUserEmailJourneyAssertion previousVerificationUserCodeHasBeenDiscarded() {
    int activeEmailChangeVerificationsCodeCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_CODE_TABLE, "code_type = 'EMAIL_CHANGE_VERIFICATION' "
            + "AND status = 'DISCARDED'");
    assertThat(activeEmailChangeVerificationsCodeCount).isPositive();
    return this;
  }

  private ChangeUserEmailJourneyAssertion thereIsOneActiveEmailChangeVerificationUserCode() {
    int activeEmailChangeVerificationsCodeCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_CODE_TABLE, "code_type = 'EMAIL_CHANGE_VERIFICATION' "
            + "AND status = 'ACTIVE'");
    assertThat(activeEmailChangeVerificationsCodeCount).isOne();
    return this;
  }

  public ChangeUserEmailJourneyAssertion userWithEmailHasBeenRemovedFromIdentityProvider(
      String email) {
    boolean userExists = identityProvider.checkIfUserExists(email);
    assertThat(userExists).isFalse();
    return this;
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

  @SneakyThrows
  private Map<String, String> readToMap(String body) {
    return objectMapper.readValue(body, new TypeReference<Map<String, String>>() {});
  }

  private InitiateEmailChangeRequest createUpdateEmailRequestDto() {
    return InitiateEmailChangeRequest.builder()
        .accountUserId(accountUserId.toString())
        .confirmUrl("http://localhost")
        .newEmail(newEmail)
        .build();
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }

  public ChangeUserEmailJourneyAssertion userWithNewEmailIsCreatedInIdentityProvider() {
    boolean userExists = identityProvider.checkIfUserExists(newEmail);
    assertThat(userExists).isTrue();
    return this;
  }

  public ChangeUserEmailJourneyAssertion pendingUserIdHasBeenSetForExistingUser() {
    int userByAccountUserIdWithPendingUserIdSet = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE, "account_user_id = '" + accountUserId + "' "
            + "AND account_user_id IS NOT NULL");
    assertThat(userByAccountUserIdWithPendingUserIdSet).isOne();
    return this;
  }

  public String getChangeEmailToken() {
    return changeEmailToken;
  }
}
