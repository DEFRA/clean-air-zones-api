package uk.gov.caz.accounts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.controller.AccountsControllerTestIT.EMAIL;
import static uk.gov.caz.accounts.controller.AccountsControllerTestIT.createUserRequest;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;

@MockedMvcIntegrationTest
public class AccountsControllerRollbackTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private IdentityProvider identityProvider;

  @Autowired
  private AccountRepository accountRepository;

  @MockBean
  private UserRepository userRepository;

  @Test
  public void shouldRollbackTransactionAndDeleteDbUserOnPersistenceIssue() throws Exception {
    String accountId = createAccount();
    when(userRepository.save(any(UserEntity.class)))
        .thenThrow(new RuntimeException("Simulate exception during persistence"));

    UserForAccountCreationRequestDto dto = createUserRequest();
    mockMvc.perform(post(AccountsController.ACCOUNTS_PATH + "/{accountId}/users", accountId)
        .content(createPayloadWithNewAccount(dto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().is5xxServerError());

    assertThatNoUsersHaveBeenCreated();
  }

  private String createAccount() {
    Account account = accountRepository
        .save(Account.builder().name("jaqu2@dev.co.uk").build());
    return account.getId().toString();
  }

  private void assertThatNoUsersHaveBeenCreated() {
    assertThat(identityProvider.checkIfUserExists(EMAIL)).isFalse();

    int usersCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "CAZ_ACCOUNT.T_ACCOUNT_USER");
    assertThat(usersCount).isZero();
  }

  @SneakyThrows
  private String createPayloadWithNewAccount(Object dto) {
    return objectMapper.writeValueAsString(dto);
  }
}
