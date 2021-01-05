package uk.gov.caz.accounts.assertion;

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
import uk.gov.caz.accounts.dto.ValidateTokenRequest;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class ValidateTokenJourneyAssertion {

  private final ObjectMapper objectMapper;

  private UUID token;

  private String correlationId;
  private Response validatePasswordResetTokenResponse;

  public ValidateTokenJourneyAssertion forToken(UUID token) {
    this.token = token;
    return this;
  }

  public ValidateTokenJourneyAssertion whenRequestToValidateToken() {
    RestAssured.basePath = PasswordController.BASE_PATH;
    this.correlationId = "7156cb1c-c7c5-4899-bcdc-f0a866d160cf";

    this.validatePasswordResetTokenResponse = RestAssured.given()
        .body(createValidateTokenRequest())
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post(PasswordController.VALIDATE_TOKEN_PATH);
    return this;
  }

  public ValidateTokenJourneyAssertion then() {
    return this;
  }

  private String createValidateTokenRequest() {
    return toJsonString(ValidateTokenRequest.builder().token(token).build());
  }

  public ValidateTokenJourneyAssertion responseIsReturnedWithHttpNoContentStatusCode() {
    validatePasswordResetTokenResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NO_CONTENT.value());
    return this;
  }

  public ValidateTokenJourneyAssertion responseIsReturnedBadRequestStatusCode() {
    validatePasswordResetTokenResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .body("message", equalTo("Token is invalid or expired"));
    return this;
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
