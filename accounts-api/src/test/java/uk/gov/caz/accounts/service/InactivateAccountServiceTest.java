package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InactivateAccountServiceTest {

  private final UUID ANY_ACCOUNT_ID = UUID.randomUUID();

  @Mock
  private UserRemovalService userRemovalService;

  @Mock
  private AccountVehicleRepository accountVehicleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private InactivateAccountService inactivateAccountService;

  @Nested
  class Preconditions {

    @Test
    public void shouldThrowExceptionWhenAccountIdIsNull() {
      // given
      UUID accountId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> inactivateAccountService.inactivateAccount(accountId));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("accountId cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenAccountIsNotFound() {
      // given
      UUID accountId = UUID.randomUUID();
      when(accountRepository.findById(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(
          () -> inactivateAccountService.inactivateAccount(accountId));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("accountId must point to an existing account");
    }

    @Test
    public void shouldThrowExceptionWhenAccountIsNotActive() {
      // given
      UUID accountId = UUID.randomUUID();
      when(accountRepository.findById(any())).thenReturn(optionalInactiveAccount());

      // when
      Throwable throwable = catchThrowable(
          () -> inactivateAccountService.inactivateAccount(accountId));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("account must be active");
    }

    private Optional<Account> optionalInactiveAccount() {
      Account account = Account.builder()
          .inactivationTimestamp(LocalDateTime.now())
          .build();
      return Optional.of(account);
    }
  }

  @Test
  public void shouldPerformAccountVehiclesBulkDelete() {
    mockActiveAccount();
    mockExistingAccountUsers();

    inactivateAccountService.inactivateAccount(ANY_ACCOUNT_ID);

    verify(accountVehicleRepository).deleteInBulkByAccountId(any());
  }

  @Test
  public void shouldPerformSequentialAccountUsersDeletion() {
    mockActiveAccount();
    mockExistingAccountUsers();
    mockValidUserRemoval();

    inactivateAccountService.inactivateAccount(ANY_ACCOUNT_ID);

    verify(userRepository).findAllActiveUsersByAccountId(ANY_ACCOUNT_ID);
    verify(userRemovalService, times(2)).removeAnyUser(any(), any());
  }

  @Test
  public void shouldNotReturnErrorWhenUserRemovalFails() {
    mockActiveAccount();
    mockExistingAccountUsers();
    mockInvalidUserRemoval();

    inactivateAccountService.inactivateAccount(ANY_ACCOUNT_ID);

    verify(userRepository).findAllActiveUsersByAccountId(ANY_ACCOUNT_ID);
    verify(userRemovalService, times(2)).removeAnyUser(any(), any());
  }

  private void mockActiveAccount() {
    Account account = Account.builder()
        .id(ANY_ACCOUNT_ID)
        .name("Sample Account")
        .build();

    when(accountRepository.findById(any())).thenReturn(Optional.of(account));
  }

  private void mockExistingAccountUsers() {
    List<UserEntity> existingUsers = Arrays.asList(
        UserEntity.builder().id(UUID.randomUUID()).accountId(UUID.randomUUID()).build(),
        UserEntity.builder().id(UUID.randomUUID()).accountId(UUID.randomUUID()).build()
    );

    when(userRepository.findAllActiveUsersByAccountId(any())).thenReturn(existingUsers);
  }

  private void mockValidUserRemoval() {
    when(userRemovalService.removeAnyUser(any(), any()))
        .thenReturn(UserRemovalStatus.SUCCESSFULLY_DELETED);
  }

  private void mockInvalidUserRemoval() {
    when(userRemovalService.removeAnyUser(any(), any()))
        .thenReturn(UserRemovalStatus.ALREADY_DELETED);
  }
}
