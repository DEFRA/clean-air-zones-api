package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.dto.AccountUpdateRequestDto;
import uk.gov.caz.accounts.dto.CreateAndInviteUserRequestDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendRequest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.service.AccountAdminUserCreatorService;
import uk.gov.caz.accounts.service.AccountCreatorService;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.AccountStandardUserCreatorService;
import uk.gov.caz.accounts.service.AccountUpdateService;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.VerificationEmailConfirmationService;
import uk.gov.caz.accounts.service.VerificationEmailResendService;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    AccountsController.class,
})
@WebMvcTest
class AccountsControllerTest {

  @MockBean
  private UserService userService;

  @MockBean
  private AccountCreatorService accountCreatorService;

  @MockBean
  private VerificationEmailConfirmationService verificationEmailConfirmationService;

  @MockBean
  private AccountAdminUserCreatorService accountAdminUserCreatorService;

  @MockBean
  private AccountStandardUserCreatorService accountStandardUserCreatorService;

  @MockBean
  private VerificationEmailResendService verificationEmailResendService;

  @MockBean
  private AccountFetcherService accountFetcherService;

  @MockBean
  private AccountUpdateService accountUpdateService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ACCOUNTS_PATH = AccountsController.ACCOUNTS_PATH;
  private static final String ACCOUNTS_UPDATE_PATH =
      AccountsController.ACCOUNTS_PATH + "/{accountId}";
  private static final String ACCOUNTS_USER_PATH = ACCOUNTS_PATH + "/{accountId}/users";
  private static final String INVITE_USER_PATH = ACCOUNTS_PATH + "/{accountId}/user-invitations";
  private static final String RESEND_PATH = ACCOUNTS_PATH +
      "/{accountId}/users/{accountUserId}/verifications";

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String VALID_EMAIL = "sample@email.com";
  private static final String VALID_PASSWORD = "p4ssw0rd";
  private static final String ANY_ACCOUNT_ID = "dad02b75-47e3-45b7-af69-accd0bfde7a7";
  private static final String NOT_EXISTING_ACCOUNT_ID = UUID.randomUUID().toString();
  private static final String ANY_ACCOUNT_NAME = "Funky Pigeon";
  private static final String ANY_USER_NAME = "Funky User Pigeon";
  private static final String ANY_VERIFICATION_URL = "http://example.com";

  @Nested
  public class ResendVerificationEmail {

    private static final String ANY_USER_ID = "71e46d21-0723-4e9c-9b6f-f7c1d67c686a";

    @Test
    public void shouldReturn400WhenVerificationUrlIsBlank() throws Exception {
      String payload = requestWithPayload("");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("verificationUrl cannot be null or empty."));
    }

    @Test
    public void shouldReturn200WhenPayloadIsValid() throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL);
      when(verificationEmailResendService.resendVerificationEmail(any(), any(), any()))
          .thenReturn(getSampleUser());

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk());
    }

    private String requestWithPayload(String verificationUrl) {
      UserVerificationEmailResendRequest request = UserVerificationEmailResendRequest
          .builder()
          .verificationUrl(verificationUrl)
          .build();

      return toJson(request);
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(post(RESEND_PATH, ANY_ACCOUNT_ID, ANY_USER_ID).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  public class CreateUserForAccount {

    @Test
    public void shouldReturn400WhenEmailIsBlank() throws Exception {
      String payload = requestWithPayload("", VALID_PASSWORD, ANY_VERIFICATION_URL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("email cannot be null or empty."));
    }

    @Test
    public void shouldReturn400WhenPasswordIsBlank() throws Exception {
      String payload = requestWithPayload(VALID_EMAIL, "", ANY_VERIFICATION_URL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("password cannot be null or empty."));
    }

    @Test
    public void shouldReturn400WhenVerificationUrlIsBlank() throws Exception {
      String payload = requestWithPayload(VALID_EMAIL, VALID_PASSWORD, "");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("verificationUrl cannot be null or empty."));
    }

    @Test
    public void shouldReturn201WhenPayloadIsValid() throws Exception {
      String payload = requestWithPayload(VALID_EMAIL, VALID_PASSWORD, ANY_VERIFICATION_URL);
      mockValidUserCreation();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isCreated());
    }

    private void mockValidUserCreation() {
      when(accountAdminUserCreatorService.createAdminUserForAccount(any(), any(), any(), any()))
          .thenReturn(Pair.of(getUserMock(), getAccountMock()));
    }

    private Account getAccountMock() {
      return Account.builder()
          .name(ANY_ACCOUNT_NAME)
          .build();
    }

    private UserEntity getUserMock() {
      return UserEntity.builder()
          .id(UUID.randomUUID())
          .accountId(UUID.fromString(ANY_ACCOUNT_ID))
          .email(VALID_EMAIL)
          .isOwner(true)
          .build();
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(post(ACCOUNTS_USER_PATH, ANY_ACCOUNT_ID).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithPayload(String email, String password, String verificationUrl) {
      UserForAccountCreationRequestDto request = UserForAccountCreationRequestDto
          .builder()
          .email(email)
          .password(password)
          .verificationUrl(verificationUrl)
          .build();

      return toJson(request);
    }
  }

  @Nested
  public class InviteUser {

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    public void shouldReturn400WhenEmailIsBlank(String email) throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL, ANY_USER_NAME,
          UUID.randomUUID().toString(), email);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("email cannot be null or empty."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    public void shouldReturn400WhenUserNameIsBlank(String username) throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL, username,
          UUID.randomUUID().toString(), VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("name cannot be null or empty."));
    }

    @Test
    public void shouldReturn400WhenUserNameIsTooLong() throws Exception {
      String username = Strings.repeat("a", CreateAndInviteUserRequestDto.NAME_MAX_LENGTH + 1);
      String payload = requestWithPayload(ANY_VERIFICATION_URL, username,
          UUID.randomUUID().toString(), VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("name is too long."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    public void shouldReturn400WhenAdministratedByIsBlank(String isAdministeredBy)
        throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL, ANY_USER_NAME,
          isAdministeredBy, VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("isAdministeredBy cannot be null or empty."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "a-b-c"})
    public void shouldReturn400WhenAdministratedByIsInvalidUuid(String isAdministeredBy)
        throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL, ANY_USER_NAME,
          isAdministeredBy, VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("isAdministeredBy should be a valid UUID value."));
    }


    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    public void shouldReturn400WhenVerificationUrlIsBlank(String verificationUrl) throws Exception {
      String payload = requestWithPayload(verificationUrl, ANY_USER_NAME,
          UUID.randomUUID().toString(), VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("verificationUrl cannot be null or empty."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123:invalid-verification-url", ":-some-invalid-url"})
    public void shouldReturn400WhenVerificationUrlIsInvalid(String verificationUrl)
        throws Exception {
      String payload = requestWithPayload(verificationUrl, ANY_USER_NAME,
          UUID.randomUUID().toString(), VALID_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("verificationUrl is not valid."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalidemail", "inva@lid@emil@.com"})
    public void shouldReturn400WhenEmailIsInvalid(String email) throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_URL, ANY_USER_NAME,
          UUID.randomUUID().toString(), email);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("email is not valid."));
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(post(INVITE_USER_PATH, ANY_ACCOUNT_ID).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }


    private String requestWithPayload(String verificationUrl, String userName,
        String administeredBy, String email) {
      return toJson(
          CreateAndInviteUserRequestDto.builder()
              .verificationUrl(verificationUrl)
              .name(userName)
              .isAdministeredBy(administeredBy)
              .email(email)
              .build()
      );
    }
  }

  @Nested
  public class UpdateAccount {

    @Test
    public void shouldReturn404WhenAccountDoesNotExist() throws Exception {
      String payload = requestWithPayload("new name");
      doThrow(new AccountNotFoundException("Account not found")).
          when(accountUpdateService)
          .updateAccountName(UUID.fromString(NOT_EXISTING_ACCOUNT_ID), "new name");

      performRequestWithPayload(payload, NOT_EXISTING_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    public void shouldReturn400WhenAccountNameIsEmpty() throws Exception {
      String payload = requestWithPayload("");

      performRequestWithPayload(payload, ANY_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Account name cannot be empty."));
    }

    @Test
    public void shouldReturn400WhenAccountNameIsMissing() throws Exception {
      String payload = requestWithPayload(null);

      performRequestWithPayload(payload, ANY_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Account name cannot be null."));
    }

    @Test
    public void shouldReturn400WhenAccountNameIsTooLong() throws Exception {
      String payload = requestWithPayload(accountNameLongerThan180Chars());

      performRequestWithPayload(payload, ANY_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Account name is too long."));
    }

    @Test
    public void shouldReturn400WhenAccountNameContainsInvalidCharacters() throws Exception {
      String payload = requestWithPayload("$# 0 name &-");

      performRequestWithPayload(payload, ANY_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message").value("Account name cannot include invalid characters."));
    }

    private ResultActions performRequestWithPayload(String payload, String accountId)
        throws Exception {
      return mockMvc.perform(patch(ACCOUNTS_UPDATE_PATH, accountId).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithPayload(String newName) {
      AccountUpdateRequestDto request = AccountUpdateRequestDto.builder()
          .accountName(newName)
          .build();

      return toJson(request);
    }

    private String accountNameLongerThan180Chars() {
      return Strings.repeat("A", 190);
    }
  }

  private UserEntity getSampleUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .accountId(UUID.fromString(ANY_ACCOUNT_ID))
        .email(VALID_EMAIL)
        .isOwner(true)
        .build();
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
