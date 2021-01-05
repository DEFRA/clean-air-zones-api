package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.accounts.controller.PasswordController;
import uk.gov.caz.accounts.dto.PasswordResetRequest;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class PasswordResetJourneyAssertion {

  private final ObjectMapper objectMapper;
  private final AmazonSQS sqsClient;
  private final String emailSqsQueueName;

  private String email;
  private String resetUrl;

  private String correlationId;
  private Response passwordResetResponse;

  public PasswordResetJourneyAssertion forEmail(String email) {
    this.email = email;
    return this;
  }

  public PasswordResetJourneyAssertion whenRequestToResetPasswordMade() {
    RestAssured.basePath = PasswordController.BASE_PATH;
    this.correlationId = "7156cb1c-c7c5-4899-bcdc-f0a866d160cf";
    this.resetUrl = "http://localhost";

    this.passwordResetResponse = RestAssured.given()
        .body(createPasswordResetRequest())
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post(PasswordController.RESET_PATH);
    return this;
  }

  public PasswordResetJourneyAssertion then() {
    return this;
  }

  public PasswordResetJourneyAssertion and() {
    return this;
  }

  public PasswordResetJourneyAssertion responseIsReturnedWithHttpNoContentStatusCode() {
    passwordResetResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NO_CONTENT.value());
    return this;
  }

  public UUID resetUrlHasBeenSentToTheUser() throws JSONException {
    List<Message> messages = receiveSqsMessages();
    assertThat(messages).isNotEmpty();
    String sendEmailBody = messages.stream().findFirst().get().getBody();
    JSONObject jsonEmailBody = new JSONObject(sendEmailBody);
    JSONObject jsonEmailVariables = new JSONObject(jsonEmailBody.get("personalisation").toString());
    String resetPasswordUrl = jsonEmailVariables.get("reset_token").toString();
    String resetPasswordToken = resetPasswordUrl.split("token=")[1];
    return UUID.fromString(resetPasswordToken);
  }

  private List<Message> receiveSqsMessages() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    ReceiveMessageResult receiveMessageResult =
        sqsClient.receiveMessage(queueUrlResult.getQueueUrl());
    return receiveMessageResult.getMessages();
  }

  private String createPasswordResetRequest() {
    return toJsonString(PasswordResetRequest.builder().email(email).resetUrl(resetUrl).build());
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
