package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.AccountVerificationRequestDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.VerificationEmailConfirmationService;
import uk.gov.caz.accounts.service.exception.PasswordInvalidException;

@MockedMvcIntegrationTest
public class AccountControllerExceptionHandlingTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private VerificationEmailConfirmationService verificationEmailConfirmationService;

  @MockBean
  private AccountRepository accountRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @InjectMocks
  private AccountsController accountsController;

  private final static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private final static UUID ANY_TOKEN = UUID.randomUUID();
  private final static String ANY_EMAIL = "jaqu@dev.co.uk";
  private final static String ANY_ACCOUNT_NAME = "account-name";
  private static final String ANY_VERIFICATION_EMAIL = "http://example.com";
  private final static String WRONG_PASSWORD = "wrong";

  @Test
  void shouldRespondWithServiceUnavailableStatusWhenUnderlyingServiceThrowsIdentityProviderUnavailableException()
      throws Exception {
    doThrow(new IdentityProviderUnavailableException("External Service Failure"))
        .when(verificationEmailConfirmationService)
        .verifyUserEmail(any());

    AccountVerificationRequestDto verificationRequestDto = AccountVerificationRequestDto.builder()
        .token(ANY_TOKEN.toString()).build();

    mockMvc.perform(post(
        AccountsController.ACCOUNTS_PATH + "/verify")
        .content(toJson(verificationRequestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().is5xxServerError())
        .andExpect(jsonPath("$.status").value(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andExpect(jsonPath("$.message").value("External Service Failure"));
  }

  @Test
  void shouldRespondWithUnprocessableEntityStatusWhenUnderlyingServiceThrowsInvalidPasswordException()
      throws Exception {
    String accountId = createAccount();
    doThrow(new PasswordInvalidException("Invalid Password"))
        .when(userService).createAdminUser(any(), any(), any());

    mockMvc.perform(
        post(AccountsController.ACCOUNTS_PATH + "/" + ANY_ACCOUNT_ID + "/users", accountId)
            .content(createUserForAccountRequestPayload())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.status").value(HttpStatus.UNPROCESSABLE_ENTITY.value()))
        .andExpect(jsonPath("$.message").value("Invalid Password"))
        .andExpect(jsonPath("$.errorCode").value("passwordNotValid"));
  }

  private String createAccount() {
    Account account = Account.builder()
        .id(ANY_ACCOUNT_ID)
        .name(ANY_ACCOUNT_NAME)
        .build();
    when(accountRepository.findById(ANY_ACCOUNT_ID)).thenReturn(Optional.of(account));
    return account.getId().toString();
  }

  private String createUserForAccountRequestPayload() {
    UserForAccountCreationRequestDto request = UserForAccountCreationRequestDto.builder()
        .email(ANY_EMAIL)
        .password(WRONG_PASSWORD)
        .verificationUrl(ANY_VERIFICATION_EMAIL)
        .build();

    return toJson(request);
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
