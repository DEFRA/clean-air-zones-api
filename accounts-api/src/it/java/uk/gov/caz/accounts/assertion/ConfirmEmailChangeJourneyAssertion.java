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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.accounts.controller.UpdateEmailController;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeRequest;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;

@RequiredArgsConstructor
public class ConfirmEmailChangeJourneyAssertion {

  private static final String ACCOUNT_USER_TABLE = "caz_account.t_account_user";
  private static final String ACCOUNT_USER_CODE_TABLE = "caz_account.t_account_user_code";

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final IdentityProvider identityProvider;

  // confirm-email parameters
  private UUID emailChangeVerificationToken;
  private String password;

  private Response confirmEmailChangeResponse;
  private String correlationId;

  public ConfirmEmailChangeJourneyAssertion forToken(UUID emailChangeVerificationToken) {
    this.emailChangeVerificationToken = emailChangeVerificationToken;
    return this;
  }

  public ConfirmEmailChangeJourneyAssertion andPassword(String password) {
    this.password = password;
    return this;
  }

  public ConfirmEmailChangeJourneyAssertion then() {
    return this;
  }

  public ConfirmEmailChangeJourneyAssertion whenRequestToConfirmEmailIsMade() {
    RestAssured.basePath = UpdateEmailController.PATH + "/change-confirm";

    this.correlationId = "9a423329-deed-4667-a162-a901ec67385d";

    this.confirmEmailChangeResponse = RestAssured.given()
        .accept(MediaType.APPLICATION_JSON.toString())
        .contentType(MediaType.APPLICATION_JSON.toString())
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .body(toJsonString(createConfirmEmailDto()))
        .when()
        .put();

    return this;
  }

  public ConfirmEmailChangeJourneyAssertion responseIsReturnedWithOkStatusAndEmail(String email) {
    confirmEmailChangeResponse.then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE)
        .header(PRAGMA_HEADER, PRAGMA_HEADER_VALUE)
        .header(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        .header(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        .header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE)
        .header(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE)
        .statusCode(HttpStatus.OK.value())
        .body("newEmail", equalTo(email));

    return this;
  }

  public ConfirmEmailChangeJourneyAssertion emailVerificationUserCodeHasBeenUsed() {
    thereIsOneUsedEmailChangeVerificationUserCode();
    return this;
  }

  private void thereIsOneUsedEmailChangeVerificationUserCode() {
    int usedEmailChangeVerificationCodeCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_CODE_TABLE, "code_type = 'EMAIL_CHANGE_VERIFICATION' "
            + "AND STATUS = 'USED'");
    assertThat(usedEmailChangeVerificationCodeCount).isOne();
  }

  public ConfirmEmailChangeJourneyAssertion userWithEmailHasBeenRemovedFromIdentityProvider(
      String email) {
    boolean userExists = identityProvider.checkIfUserExists(email);
    assertThat(userExists).isFalse();
    return this;
  }

  public ConfirmEmailChangeJourneyAssertion identityProviderUserHasBeenReassigned() {
    userWithChangedEmailHasPendingUserIdSetToNull();
    return this;
  }

  private void userWithChangedEmailHasPendingUserIdSetToNull() {
    int usersWithPendingUserIdsCount = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
        ACCOUNT_USER_TABLE, "pending_user_id IS NOT NULL");
    assertThat(usersWithPendingUserIdsCount).isZero();
  }

  private ConfirmEmailChangeRequest createConfirmEmailDto() {
    return ConfirmEmailChangeRequest.builder()
        .emailChangeVerificationToken(emailChangeVerificationToken.toString())
        .password(password)
        .build();
  }

  @SneakyThrows
  private String toJsonString(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
