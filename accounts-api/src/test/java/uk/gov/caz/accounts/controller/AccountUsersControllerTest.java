package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.UserPermissionsService;
import uk.gov.caz.accounts.service.UserPermissionsUpdaterService;
import uk.gov.caz.accounts.service.UserRemovalService;
import uk.gov.caz.accounts.service.UserRenameService;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    AccountUsersController.class
})
@WebMvcTest
public class AccountUsersControllerTest {

  @MockBean
  private AccountFetcherService accountFetcherService;

  @MockBean
  private UserService userService;

  @MockBean
  private UserRemovalService userRemovalService;

  @MockBean
  private UserPermissionsUpdaterService userPermissionsUpdaterService;

  @MockBean
  private UserRenameService userRenameService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String ANY_ACCOUNT_ID = "b6968560-cb56-4248-9f8f-d75b0aff726e";
  private static final String ANY_ACCOUNT_USER_ID = "a442c860-4602-4499-a5ec-a74fec6d084c";

  @Nested
  public class RetrievingAllUsersForAccountId {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      mockMvc.perform(
          get(AccountUsersController.USERS_PATH, ANY_ACCOUNT_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn400WhenAccountIdIsNotValidUuid() throws Exception {
      String invalidAccountId = "TEST";

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH, invalidAccountId)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn404WhenAccountIsNotFound() throws Exception {
      mockAccountNotFoundResponse();
      mockUsersForAccount();

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH, ANY_ACCOUNT_ID)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn200WhenVrnIsFound() throws Exception {
      mockAccountFoundResponse();

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH, ANY_ACCOUNT_ID)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk());
    }

    private void mockAccountNotFoundResponse() {
      when(accountFetcherService.findById(any()))
          .thenReturn(Optional.empty());
    }

    private void mockAccountFoundResponse() {
      Account foundAccount = Account.builder()
          .id(UUID.fromString(ANY_ACCOUNT_ID))
          .multiPayerAccount(true)
          .build();

      when(accountFetcherService.findById(UUID.fromString(ANY_ACCOUNT_ID)))
          .thenReturn(Optional.ofNullable(foundAccount));
    }

    private void mockUsersForAccount() {
      UserEntity foundUser = UserEntity.builder()
          .id(UUID.randomUUID())
          .accountId(UUID.fromString(ANY_ACCOUNT_ID))
          .identityProviderUserId(UUID.randomUUID())
          .email("any@email.com")
          .name("Any Name")
          .build();

      when(userService.getAllUsersForAccountId(UUID.fromString(ANY_ACCOUNT_ID))).thenReturn(
          Arrays.asList(foundUser));
    }
  }

  @Nested
  public class RetrievingSingleUser {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      mockMvc.perform(
          get(AccountUsersController.USERS_PATH + "/{accountUserId}", ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn400WhenAccountIdIsNotValidUuid() throws Exception {
      String invalidAccountId = "TEST";

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH + "/{accountUserId}", invalidAccountId,
              ANY_ACCOUNT_USER_ID)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400WhenAccountUserIdIsNotValidUuid() throws Exception {
      String invalidAccountUserId = "TEST";

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH + "/{accountUserId}", ANY_ACCOUNT_ID, invalidAccountUserId)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn404WhenAccountUserIsNotFound() throws Exception {
      mockAccountUserNotFoundResponse();

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH + "/{accountUserId}", ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn200WhenVrnIsFound() throws Exception {
      mockAccountUserFoundResponse();

      mockMvc.perform(
          get(AccountUsersController.USERS_PATH + "/{accountUserId}", ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk());
    }

    private void mockAccountUserNotFoundResponse() {
      when(userService.getUserForAccountId(any(), any()))
          .thenReturn(Optional.empty());
    }

    private void mockAccountUserFoundResponse() {
      AccountPermission accountPermission = AccountPermission.builder()
        .name(Permission.MAKE_PAYMENTS)
        .description("make payments")
        .build();

      UserEntity foundUser = UserEntity.builder()
          .id(UUID.fromString(ANY_ACCOUNT_USER_ID))
          .accountId(UUID.fromString(ANY_ACCOUNT_ID))
          .identityProviderUserId(UUID.randomUUID())
          .accountPermissions(Collections.singletonList(accountPermission))
          .build();

      when(userService.getUserForAccountId(UUID.fromString(ANY_ACCOUNT_ID),
          UUID.fromString(ANY_ACCOUNT_USER_ID))).thenReturn(Optional.ofNullable(foundUser));
    }
  }
}