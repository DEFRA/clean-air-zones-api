package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
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
import uk.gov.caz.accounts.service.emailnotifications.AccountCloseEmailSender;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

@ExtendWith(MockitoExtension.class)
public class AccountCloseServiceTest {

  private static final AccountClosureReason CLOSURE_REASON = AccountClosureReason.OTHER;
  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final String ANY_EMAIL = "any@email.com";

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private InactivateAccountService inactivateAccountService;

  @Mock
  private AccountCloseEmailSender accountCloseEmailSender;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private AccountCloseService accountCloseService;

  @Test
  public void shouldCloseAccountWhenParametersAreValid() {
    // given
    accountIsPresent();
    mockExistingUsersForAccount();
    when(identityProvider.getEmailByIdentityProviderId(any())).thenReturn(ANY_EMAIL);

    // when
    accountCloseService.closeAccount(ACCOUNT_ID, CLOSURE_REASON.toString());

    // then
    verify(accountRepository).updateClosureReason(ACCOUNT_ID, CLOSURE_REASON);
    verify(inactivateAccountService).inactivateAccount(ACCOUNT_ID);
    verify(accountCloseEmailSender, times(2)).send(anyString(), any());
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenClosureReasonIsNull() {
    // when
    Throwable throwable = catchThrowable(
        () -> accountCloseService.closeAccount(ACCOUNT_ID, null));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("'closureReason' cannot be null");
  }

  @Test
  public void shouldThrowAccountNotFoundExceptionWhenAccountIsNotPresent() {
    // given
    accountIsNotPresent();

    // when
    Throwable throwable = catchThrowable(
        () -> accountCloseService.closeAccount(ACCOUNT_ID, "does not matter"));

    // then
    assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account was not found.");
  }

  @Test
  public void shouldNotThrowExceptionWhenLocalUserWasNotFoundInTheIdentityProvider() {
    // given
    accountIsPresent();
    mockExistingUsersForAccount();
    doThrow(new IdentityProviderUnavailableException())
        .when(identityProvider).getEmailByIdentityProviderId(any());

    // when
    Throwable throwable = catchThrowable(
        () -> accountCloseService.closeAccount(ACCOUNT_ID, CLOSURE_REASON.toString()));

    // then
    assertThat(throwable).isNull();
  }

  private void accountIsPresent() {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(Account.builder().build()));
  }

  private void accountIsNotPresent() {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
  }

  private void mockExistingUsersForAccount() {
    UserEntity owner1 = UserEntity.builder()
        .accountId(UUID.randomUUID())
        .id(UUID.randomUUID())
        .isOwner(true)
        .build();

    UserEntity owner2 = UserEntity.builder()
        .accountId(UUID.randomUUID())
        .id(UUID.randomUUID())
        .isOwner(true)
        .build();

    when(userRepository.findAllActiveUsersByAccountId(any())).thenReturn(
        Arrays.asList(owner1, owner2));
  }
}
