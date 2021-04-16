package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountClosureReason;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor165DaysEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor175DaysEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor180DaysEmailSender;

@ExtendWith(MockitoExtension.class)
class ProcessInactiveUsersServiceTest {

  private static final AccountClosureReason REASON = AccountClosureReason.ACCOUNT_INACTIVITY;
  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final String ANY_COMPANY_NAME = "Funky Pigeon";
  private static final String ANY_EMAIL = "sample@email.com";

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private InactivateAccountService inactivateAccountService;

  @Mock
  private InactiveFor165DaysEmailSender inactiveFor165DaysEmailSender;

  @Mock
  private InactiveFor175DaysEmailSender inactiveFor175DaysEmailSender;

  @Mock
  private InactiveFor180DaysEmailSender inactiveFor180DaysEmailSender;

  @InjectMocks
  private ProcessInactiveUsersService processInactiveUsersService;

  @Nested
  class WhenAllAccountsAreInactivated {

    @Test
    public void shouldNotProcessAnyAccount() {
      // given
      mockInactiveAccounts();

      // when
      processInactiveUsersService.execute();

      // then
      verify(userRepository, never()).getLatestUserSignInForAccount(any());
    }

    private void mockInactiveAccounts() {
      when(accountRepository.findAllByInactivationTimestampIsNull())
          .thenReturn(Collections.emptyList());
    }
  }

  @Nested
  class WhenNoAccountRequireNotification {

    @Test
    public void shouldNotCallAnyNotificationSender() {
      // given
      mockActiveAccountWhichDoesNotRequireNotification();

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor180DaysEmailSender, never()).send(anyString(), any());
    }

    private void mockActiveAccountWhichDoesNotRequireNotification() {
      Account account = Account.builder()
          .id(UUID.randomUUID())
          .name(ANY_COMPANY_NAME)
          .build();

      when(accountRepository.findAllByInactivationTimestampIsNull())
          .thenReturn(Arrays.asList(account));
      when(userRepository.getLatestUserSignInForAccount(any()))
          .thenReturn(Optional.of(Timestamp.valueOf(LocalDateTime.now())));
    }
  }

  @Nested
  class WhenAccountHasUsersWhoNeverSignedIn {

    @Test
    public void shouldNotCallAnyNotificationSender() {
      // given
      mockAccountWithUsersWhoNeverSignedIn();

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor180DaysEmailSender, never()).send(anyString(), any());
    }

    private void mockAccountWithUsersWhoNeverSignedIn() {
      Account account = Account.builder()
          .id(UUID.randomUUID())
          .name(ANY_COMPANY_NAME)
          .build();

      when(accountRepository.findAllByInactivationTimestampIsNull())
          .thenReturn(Arrays.asList(account));
      when(userRepository.getLatestUserSignInForAccount(any()))
          .thenReturn(Optional.empty());
    }
  }

  @Nested
  class WhenAccountWasNotActiveFor165Days {

    @Test
    public void shouldCallInactiveFor165DaysEmailSender() {
      // given
      mockAccountInactiveForDaysCount(165);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender, never()).send(anyString(), any());
      verify(accountRepository, never()).updateClosureReason(any(), any());
      verify(inactiveFor180DaysEmailSender, never()).send(anyString(), any());
      verify(inactivateAccountService, never()).inactivateAccount(any());
    }
  }

  @Nested
  class WhenAccountWasNotActiveFor175Days {

    @Test
    public void shouldCallInactiveFor175DaysEmailSender() {
      // given
      mockAccountInactiveForDaysCount(175);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender).send(anyString(), any());
      verify(accountRepository, never()).updateClosureReason(any(), any());
      verify(inactiveFor180DaysEmailSender, never()).send(anyString(), any());
      verify(inactivateAccountService, never()).inactivateAccount(any());
    }
  }

  @Nested
  class WhenAccountWasNotActiveFor180Days {

    @Test
    public void shouldCallInactiveFor180DaysEmailSender() {
      // given
      mockAccountInactiveForDaysCount(180);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender, never()).send(anyString(), any());
      verify(accountRepository).updateClosureReason(ACCOUNT_ID, REASON);
      verify(inactiveFor180DaysEmailSender).send(anyString(), any());
    }

    @Test
    public void shouldInactivateTheAccount() {
      // given
      mockAccountInactiveForDaysCount(180);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactivateAccountService).inactivateAccount(any());
    }
  }

  @Nested
  class WhenAccountWasNotActiveFor181Days {

    @Test
    public void shouldCallInactiveFor180DaysEmailSender() {
      // given
      mockAccountInactiveForDaysCount(181);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactiveFor165DaysEmailSender, never()).send(anyString(), any());
      verify(inactiveFor175DaysEmailSender, never()).send(anyString(), any());
      verify(accountRepository).updateClosureReason(ACCOUNT_ID, REASON);
      verify(inactiveFor180DaysEmailSender).send(anyString(), any());
    }

    @Test
    public void shouldInactivateTheAccount() {
      // given
      mockAccountInactiveForDaysCount(181);

      // when
      processInactiveUsersService.execute();

      // then
      verify(inactivateAccountService).inactivateAccount(any());
    }
  }

  @Nested
  class WhenLocalAccountWasNotFoundInIdentityProvider {

    @Test
    public void shouldNotThrowException() {
      // given
      mockAccountWithInconsistentState();

      // when
      Throwable throwable = catchThrowable(() -> processInactiveUsersService.execute());

      // then
      assertThat(throwable).isNull();
    }

    private void mockAccountWithInconsistentState() {
      Account account = Account.builder().id(ACCOUNT_ID).name(ANY_COMPANY_NAME).build();
      UserEntity owner = UserEntity.builder()
          .identityProviderUserId(UUID.randomUUID())
          .email(ANY_EMAIL)
          .build();

      when(accountRepository.findAllByInactivationTimestampIsNull())
          .thenReturn(Arrays.asList(account));
      when(userRepository.getLatestUserSignInForAccount(any()))
          .thenReturn(Optional.of(Timestamp.valueOf(LocalDateTime.now().minusDays(180))));
      when(userRepository.findOwnersForAccount(any()))
          .thenReturn(Arrays.asList(owner));
      doThrow(new IdentityProviderUnavailableException()).when(identityProvider)
          .getEmailByIdentityProviderId(any());
    }
  }


  private void mockAccountInactiveForDaysCount(int daysCount) {
    Account account = Account.builder().id(ACCOUNT_ID).name(ANY_COMPANY_NAME).build();
    UserEntity owner = UserEntity.builder()
        .identityProviderUserId(UUID.randomUUID())
        .email(ANY_EMAIL)
        .build();

    when(accountRepository.findAllByInactivationTimestampIsNull())
        .thenReturn(Arrays.asList(account));
    when(userRepository.getLatestUserSignInForAccount(any()))
        .thenReturn(Optional.of(Timestamp.valueOf(LocalDateTime.now().minusDays(daysCount))));
    when(userRepository.findOwnersForAccount(any()))
        .thenReturn(Arrays.asList(owner));
    when(identityProvider.getEmailByIdentityProviderId(any()))
        .thenReturn(ANY_EMAIL);
  }
}
