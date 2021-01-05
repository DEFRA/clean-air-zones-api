package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import java.util.List;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendRequest;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class ResendVerificationEmailJourneyAssertion {

  private final ObjectMapper objectMapper;
  private final AmazonSQS sqsClient;
  private final String emailSqsQueueName;
  private final JdbcTemplate jdbcTemplate;

  private String accountId;
  private String accountUserId;
  private String verificationUrl;

  private String correlationId;

  public ResendVerificationEmailJourneyAssertion forAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public ResendVerificationEmailJourneyAssertion forAccountUserId(String accountUserId) {
    this.accountUserId = accountUserId;
    return this;
  }

  public ResendVerificationEmailJourneyAssertion forVerificationUrl(String verificationUrl) {
    this.verificationUrl = verificationUrl;
    return this;
  }

  public ResendVerificationEmailJourneyAssertion then() {
    return this;
  }

  public ResendVerificationEmailJourneyAssertion whenRequestToResendVerificationEmailIsMade() {
    this.correlationId = "f11281a2-306b-11ea-b08d-737c67689306";

    RestAssured.given()
        .body(resendVerificationEmailRequestPayload())
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post(AccountsController.RESEND_VERIFICATION_EMAIL_PATH, accountId, accountUserId);

    return this;
  }

  @SneakyThrows
  public ResendVerificationEmailJourneyAssertion verificationEmailWasSent() {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).isNotEmpty();
    String sendEmailBody = messages.iterator().next().getBody();
    JSONObject jsonEmailBody = new JSONObject(sendEmailBody);
    JSONObject jsonEmailVariables = new JSONObject(jsonEmailBody.get("personalisation").toString());
    String verificationUrl = jsonEmailVariables.get("link").toString();
    String verificationToken = verificationUrl.split("token=")[1];
    assertThat(verificationToken).isNotBlank();

    return this;
  }

  public ResendVerificationEmailJourneyAssertion oldVerificationCodeWasDiscarded() {
    int discardedCodesCount = JdbcTestUtils
        .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_user_code",
            "status = 'DISCARDED'");
    int activeCodesCount = JdbcTestUtils
        .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account_user_code",
            "status = 'ACTIVE'");

    assertThat(discardedCodesCount).isEqualTo(1);
    assertThat(activeCodesCount).isEqualTo(1);

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

  private String resendVerificationEmailRequestPayload() {
    UserVerificationEmailResendRequest request = UserVerificationEmailResendRequest.builder()
        .verificationUrl(verificationUrl)
        .build();

    return toJsonString(request);
  }

  @SneakyThrows
  private String toJsonString(Object object) {
    return objectMapper.writeValueAsString(object);
  }
}
