package uk.gov.caz.accounts.assertion;

import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class VerifyUserAccountJourneyAssertion {

  private final ObjectMapper objectMapper;
  private UUID token;

  private String correlationId;
  private Response verifyAccountResponse;

  public VerifyUserAccountJourneyAssertion forVerificationToken(UUID token) {
    this.token = token;
    return this;
  }

  public VerifyUserAccountJourneyAssertion then() {
    return this;
  }

  @SneakyThrows
  public VerifyUserAccountJourneyAssertion whenRequestToVerifyAccountByTokenIsMade() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
    this.correlationId = "f11281a2-306b-11ea-b08d-737c67689306";

    this.verifyAccountResponse = RestAssured.given()
        .body(objectMapper.writeValueAsString(Collections.singletonMap("token", token)))
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post("/verify");
    return this;
  }


  public VerifyUserAccountJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    verifyAccountResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value());
    return this;
  }

  public VerifyUserAccountJourneyAssertion responseIsReturnedWithHttpUnprocessableEntityStatusCode() {
    verifyAccountResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    return this;
  }

  public VerifyUserAccountJourneyAssertion andMessageIsEqualTo(String message) {
    verifyAccountResponse.then()
        .body("message", equalTo(message));
    return this;
  }

  public VerifyUserAccountJourneyAssertion andErrorCodeIsEqualTo(String errorCode) {
    verifyAccountResponse.then()
        .body("errorCode", equalTo(errorCode));
    return this;
  }
}
