package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.LoginController;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.dto.LoginResponseDto;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class LoginUserJourneyAssertion {

  private static final String ACCOUNT_USER_TABLE = "caz_account.t_account_user";

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  private String email;
  private String password;

  private String correlationId;
  private Response loginResponse;
  private LoginResponseDto loginResponseDto;

  public LoginUserJourneyAssertion forEmail(String email) {
    this.email = email;
    return this;
  }

  public LoginUserJourneyAssertion andPassword(String password) {
    this.password = password;
    return this;
  }

  public LoginUserJourneyAssertion whenRequestToLogInIsMade() {
    RestAssured.basePath = LoginController.LOGIN_PATH;
    this.correlationId = "7156cb1c-c7c5-4899-bcdc-f0a866d160cf";

    this.loginResponse = RestAssured.given()
        .body(createLogInRequest())
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .when()
        .post();
    return this;
  }

  public LoginUserJourneyAssertion then() {
    return this;
  }

  public LoginUserJourneyAssertion responseIsReturnedWithHttpOkStatusCode() {
    this.loginResponseDto = loginResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.OK.value())
        .extract()
        .as(LoginResponseDto.class);
    return this;
  }

  public LoginUserJourneyAssertion responseIsReturnedWithHttpUnauthorisedStatusCode() {
    loginResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    return this;
  }

  public LoginUserJourneyAssertion withAccountNameEqualTo(String accountName) {
    assertThat(loginResponseDto.getAccountName()).isEqualTo(accountName);
    return this;
  }

  public LoginUserJourneyAssertion withEmailEqualTo(String email) {
    assertThat(loginResponseDto.getEmail()).isEqualTo(email);
    return this;
  }

  public LoginUserJourneyAssertion withAccountIdEqualTo(String accountId) {
    assertThat(loginResponseDto.getAccountId()).isEqualTo(UUID.fromString(accountId));
    return this;
  }

  public LoginUserJourneyAssertion withAccountUserIdEqualTo(String accountUserId) {
    assertThat(loginResponseDto.getAccountUserId()).isEqualTo(UUID.fromString(accountUserId));
    return this;
  }

  public LoginUserJourneyAssertion withAdminEqualToTrue() {
    assertThat(loginResponseDto.isOwner()).isTrue();
    return this;
  }

  public LoginUserJourneyAssertion accountUserIsUpdatedInDatabaseWithSignInTimeStamp() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "LAST_SIGN_IN_TIMESTMP IS NOT NULL AND LAST_SIGN_IN_TIMESTMP > now() - INTERVAL '1 MINUTE'");

    assertThat(accountRowsCount).isEqualTo(1);
    return this;
  }

  public LoginUserJourneyAssertion withPasswordUpdateTimestamp(
      LocalDateTime expectedPasswordUpdateTimestamp) {
    assertThat(loginResponseDto.getPasswordUpdateTimestamp())
        .isEqualToIgnoringNanos(expectedPasswordUpdateTimestamp);
    return this;
  }

  public LoginUserJourneyAssertion accountUserIsNotUpdatedInDatabaseWithSignInTimeStamp() {
    int accountRowsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE,
        "LAST_SIGN_IN_TIMESTMP IS NULL");

    assertThat(accountRowsCount).isEqualTo(1);
    return this;
  }

  private String createLogInRequest() {
    return toJsonString(LoginRequestDto.builder().email(email).password(password).build());
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
