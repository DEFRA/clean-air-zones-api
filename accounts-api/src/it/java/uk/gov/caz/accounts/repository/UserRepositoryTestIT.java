package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.UserEntity;

@Sql(scripts = {
    "classpath:data/sql/create-account-user.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {
    "classpath:data/sql/delete-user-data.sql"}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
class UserRepositoryTestIT {

  private final static UUID EXISTING_ACCOUNT_ID = UUID
      .fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");
  private final static UUID EXISTING_ACCOUNT_USER_ID = UUID
      .fromString("4e581c88-3ba3-4df0-91a3-ad46fb48bfd1");

  @Autowired
  private UserRepository userRepository;


  @Test
  public void shouldReturnAllNonDeletedUsersForTheProvidedAccountId() {
    List<UserEntity> foundUsers = userRepository
        .findAllActiveUsersByAccountId(EXISTING_ACCOUNT_ID);

    assertThat(foundUsers.size()).isEqualTo(5);
  }

  @Test
  public void shouldReturnOwnerUsersForAccount() {
    List<UserEntity> ownerUsers = userRepository
        .findOwnersForAccount(EXISTING_ACCOUNT_ID);

    assertThat(ownerUsers.size()).isEqualTo(2);
  }

  @Test
  @Transactional
  public void shouldUpdateLastSingInTimestamp() {
    userRepository.setLastSingInTimestamp(EXISTING_ACCOUNT_USER_ID);
    UserEntity user = userRepository.findById(EXISTING_ACCOUNT_USER_ID).get();

    assertThat(user.getLastSingInTimestmp()).isNotNull();
  }
}
