package uk.gov.caz.accounts.assertion;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.UUID;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.accounts.controller.PasswordController;
import uk.gov.caz.accounts.dto.UpdatePasswordRequest;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;


public class UpdatePasswordJourneyAssertion {

  private final ObjectMapper objectMapper;
  private final IdentityProvider identityProvider;

  private UUID accountUserId;
  private String oldPassword;
  private String newPassword;
  private String correlationId;
  private Response updatePasswordResponse;

  public UpdatePasswordJourneyAssertion(ObjectMapper objectMapper,
      IdentityProvider identityProvider, UUID accountUserId) {
    this.objectMapper = objectMapper;
    this.identityProvider = identityProvider;
    this.accountUserId = accountUserId;
  }

  public UpdatePasswordJourneyAssertion whenRequestToUpdatePasswordIsMade() {
    RestAssured.basePath = PasswordController.BASE_PATH + PasswordController.UPDATE_PATH;
    this.correlationId = "0494b1a1-41de-49d2-86ea-0774663b98f0";

    this.updatePasswordResponse = given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(toJsonString(createUpdateRequest(newPassword)))
        .when()
        .post();
    return this;
  }

  public UpdatePasswordJourneyAssertion thenNoContentResponseIsReceived() {
    updatePasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.NO_CONTENT.value());
    return this;
  }

  private UpdatePasswordRequest createUpdateRequest(String newPassword) {
    return UpdatePasswordRequest.builder()
        .accountUserId(accountUserId)
        .oldPassword(oldPassword)
        .newPassword(newPassword)
        .build();
  }

  public UpdatePasswordJourneyAssertion forOldPassword(String password) {
    this.oldPassword = password;
    return this;
  }

  public UpdatePasswordJourneyAssertion forNewPassword(String password) {
    this.newPassword = password;
    return this;
  }

  public UpdatePasswordJourneyAssertion forAccountUserId(UUID accountUserId) {
    this.accountUserId = accountUserId;
    return this;
  }

//  public UpdatePasswordJourneyAssertion exceptionIsThrownDuringLogin(Exception exception) {
//    when(identityProvider.loginUser(anyString(), anyString()).thenThrow(exception);
//    return this;
//  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }

  public UpdatePasswordJourneyAssertion then422ResponseIsReturned() {
    updatePasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    return this;
  }

  public UpdatePasswordJourneyAssertion then500ResponseIsReturned() {
    updatePasswordResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    return this;
  }

  public UpdatePasswordJourneyAssertion andMessageEqualTo(String message) {
    updatePasswordResponse.then().body("message", equalTo(message));
    return this;
  }

  public UpdatePasswordJourneyAssertion andErrorCodeEqualTo(String errorCode) {
    updatePasswordResponse.then().body("errorCode", equalTo(errorCode));
    return this;
  }
}
