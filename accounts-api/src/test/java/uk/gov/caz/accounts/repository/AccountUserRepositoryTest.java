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
}
