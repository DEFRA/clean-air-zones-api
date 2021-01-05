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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.AccountsController;
import uk.gov.caz.accounts.dto.AccountCreationRequestDto;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class CreateAccountJourneyAssertion {

  private static final String ACCOUNT_TABLE = "caz_account.t_account";
  private static final String ACCOUNT_USER_TABLE = "caz_account.t_account_user";

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  private AccountCreationResponseDto creationResponseDto;

  // create-account parameters
  private String accountName;

  private Response createAccountResponse;
  private String correlationId;

  public CreateAccountJourneyAssertion forAccountWithName(String accountName) {
    this.accountName = accountName;
    return this;
  }

  public CreateAccountJourneyAssertion whenRequestToCreateAccountIsMade() {
    RestAssured.basePath = AccountsController.ACCOUNTS_PATH;
    this.correlationId = "79b7a48f-27c7-4947-bd1c-670f981843ef";

    this.createAccountResponse = RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(toJsonString(createAccountCreationRequestDto()))
        .when()
        .post();
    return this;
  }

  public CreateAccountJourneyAssertion then() {
    return this;
  }

  public CreateAccountJourneyAssertion responseIsReturnedWithHttpCreatedStatusCode() {
    this.creationResponseDto = createAccountResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .as(AccountCreationResponseDto.class);
    return this;
  }

  public CreateAccountJourneyAssertion responseIsReturnedWithHttpUnprocessableEntityStatusCode() {
    createAccountResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    return this;
  }

  public CreateAccountJourneyAssertion andMessageEqualTo(String message) {
    createAccountResponse.then().body("message", equalTo(message));
    return this;
  }

  public CreateAccountJourneyAssertion andErrorCodeEqualTo(String errorCode) {
    createAccountResponse.then().body("errorCode", equalTo(errorCode));
    return this;
  }

  public CreateAccountJourneyAssertion andNoNewRecordsAreCreatedInDatabase() {
    verifyThatSingleUserDataAreStoredInDatabase();
    return this;
  }

  private AccountCreationRequestDto createAccountCreationRequestDto() {
    return AccountCreationRequestDto.builder()
        .accountName(accountName)
        .build();
  }

  public AccountCreationResponseDto getCreationResponseDto() {
    return creationResponseDto;
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }

  private void verifyThatSingleUserDataAreStoredInDatabase() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_TABLE,
        "account_name = '" + accountName + "'");
    int accountUserRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "is_administrator = TRUE");

    assertThat(accountRowsCount).isEqualTo(1);
    assertThat(accountUserRowsCount).isEqualTo(0);
  }
}
