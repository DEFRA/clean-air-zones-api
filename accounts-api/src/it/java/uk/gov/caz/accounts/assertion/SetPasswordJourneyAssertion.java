package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.accounts.controller.PasswordController;
import uk.gov.caz.accounts.dto.SetPasswordRequest;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class SetPasswordJourneyAssertion {

  private final ObjectMapper objectMapper;
  private final IdentityProvider identityProvider;

  private UUID token;
  private String password;

  private String correlationId;
  private Response setPasswordResponse;

  public SetPasswordJourneyAssertion forToken(UUID token) {
    this.token = token;
    return this;
  }

  public SetPasswordJourneyAssertion forPassword(String password) {
    this.password = password;
    return this;
  }

  public SetPasswordJourneyAssertion whenRequestToSetPassword() {
    RestAssured.basePath = PasswordController.BASE_PATH;
    this.correlationId = "7156cb1c-c7c5-4899-bcdc-f0a866d160cf";

    this.setPasswordResponse = RestAssured.given()
        .body(createSetPasswordTokenRequest())
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .put(PasswordController.SET_PATH);
    return this;
  }

  public SetPasswordJourneyAssertion then() {
    return this;
  }

  public SetPasswordJourneyAssertion responseIsReturnedWithHttpNoContentStatusCode() {
    setPasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NO_CONTENT.value());
    return this;
  }

  public SetPasswordJourneyAssertion and() {
    return this;
  }

  public SetPasswordJourneyAssertion userEmailHasBeenVerified(String email) {
    assertThat(identityProvider.getUser(email).isEmailVerified()).isTrue();
    return this;
  }

  public SetPasswordJourneyAssertion responseIsReturnedBadRequestStatusCode() {
    setPasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .body("message", equalTo("Token is invalid or expired"));
    return this;
  }

  public SetPasswordJourneyAssertion responseIs422WithMessage(String message) {
    setPasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
        .body("message", equalTo(message));
    return this;
  }

  private String createSetPasswordTokenRequest() {
    return toJsonString(SetPasswordRequest.builder().token(token).password(password).build());
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
