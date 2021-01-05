package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.Account;

@Sql(scripts = {
    "classpath:data/sql/add-account-multipayer.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {
    "classpath:data/sql/delete-user-data.sql"}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
class AccountRepositoryTestIT {

  public static final String MUTLIPAYER_ACCOUNT_ID = "457a23f1-3df9-42b9-a42e-435aef201d93";
  public static final String NON_MUTLIPAYER_ACCOUNT_ID = "457a23f1-3df9-42b9-a42e-435aef201d92";

  @Autowired
  private AccountRepository accountRepository;

  @Test
  public void shouldProperlyAccountAsMultipayerOne() {
    Account account = accountRepository.findById(UUID.fromString(MUTLIPAYER_ACCOUNT_ID)).get();

    assertThat(account.isMultiPayerAccount()).isTrue();
  }

  @Test
  public void shouldProperlyAccountAsNonMultipayerOne() {
    Account account = accountRepository.findById(UUID.fromString(NON_MUTLIPAYER_ACCOUNT_ID)).get();

    assertThat(account.isMultiPayerAccount()).isFalse();
  }
}
