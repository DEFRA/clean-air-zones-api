package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserRepository.UserRowMapper;
import uk.gov.caz.accounts.repository.exception.NotUniqueUserIdForAccountUserException;

@ExtendWith(MockitoExtension.class)
class AccountUserRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private AccountUserRepository accountUserRepository;

  @Nested
  class Insert {

    @Test
    public void shouldThrowNullPointerExceptionWhenUserIsNull() {
      // given
      User user = null;

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.insert(user));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("User cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenUserHasIdAssigned() {
      // given
      User user = User.builder().id(UUID.randomUUID()).build();

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.insert(user));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
      assertThat(throwable).hasMessage("User cannot have ID");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenUserHasNoIdentityProviderAccountCreated() {
      // given
      User user = User.builder().build();

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.insert(user));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("User need to have identity provider account created");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenUserHasNoAccountIdAssigned() {
      // given
      User user = User.builder().identityProviderUserId(UUID.randomUUID()).accountId(null).build();

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.insert(user));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("User need to be assigned to Account");
    }
  }

  @Nested
  class GetUserDetails {

    @Test
    public void shouldThrowNullPointerExceptionWhenUserIdIsNull() {
      // given
      UUID userId = null;

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.findByUserId(userId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("userId cannot be null");
    }

    @Test
    public void shouldReturnNotUniqueUserIdForAccountUserExceptionWhenMultipleRecordsFound() {
      // given
      UUID uuid = UUID.randomUUID();
      mockMultipleAccountUsersForSingleUserId(uuid);

      // when
      Throwable throwable = catchThrowable(() -> accountUserRepository.findByUserId(uuid));

      // then
      assertThat(throwable).isInstanceOf(NotUniqueUserIdForAccountUserException.class);
      assertThat(throwable).hasMessage("More than one AccountUser with same userId found");
    }

    @Test
    public void shouldReturnOptionalEmptyIfUserByUserIdNotFound() {
      // given
      UUID uuid = UUID.randomUUID();
      mockQueryByUserId(Collections.emptyList());

      // when
      Optional<User> user = accountUserRepository.findByUserId(uuid);

      // then
      assertThat(user).isEmpty();
    }

    private void mockMultipleAccountUsersForSingleUserId(UUID uuid) {
      List<User> users = Arrays.asList(
          User.builder().identityProviderUserId(uuid).build(),
          User.builder().identityProviderUserId(uuid).build()
      );

      mockQueryByUserId(users);
    }

    private void mockQueryByUserId(List<User> users) {
      when(jdbcTemplate.query(
          eq(AccountUserRepository.SELECT_BY_USER_ID_SQL),
          any(PreparedStatementSetter.class),
          any(UserRowMapper.class)
      )).thenReturn(users);
    }
  }

  @Nested
  class FindAllUsersByAccountId {

    @Test
    public void shouldThrowNullPointerExceptionWhenAccountIdIsNull() {
      // given
      UUID accountId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> accountUserRepository.findAllUsersByAccountId(accountId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("accountId cannot be null");
    }

    @Test
    public void shouldReturnEmptyListIfUserByAccountIdNotFound() {
      // given
      UUID accountId = UUID.randomUUID();
      mockQueryStandardUsersByAccountId(Collections.emptyList());

      // when
      List<User> users = accountUserRepository.findAllUsersByAccountId(accountId);

      // then
      assertThat(users).isEmpty();
    }

    @Test
    public void shouldReturnFoundListIfUserByAccountIdExistsInDB() {
      // given
      UUID accountId = UUID.randomUUID();
      User user = User.builder()
          .id(UUID.randomUUID())
          .identityProviderUserId(UUID.randomUUID())
          .build();
      mockQueryStandardUsersByAccountId(Arrays.asList(user));

      // when
      List<User> users = accountUserRepository.findAllUsersByAccountId(accountId);

      // then
      assertThat(users).isNotEmpty();
      assertThat(users).contains(user);
    }

    private void mockQueryStandardUsersByAccountId(List<User> users) {
      when(jdbcTemplate.query(
          eq(AccountUserRepository.SELECT_ALL_USERS_BY_ACCOUNT_ID_SQL),
          any(PreparedStatementSetter.class),
          any(UserRowMapper.class)
      )).thenReturn(users);
    }
  }
}
