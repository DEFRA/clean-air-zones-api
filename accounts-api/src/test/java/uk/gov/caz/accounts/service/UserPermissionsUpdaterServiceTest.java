package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserPermissionsUpdaterServiceTest {

  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Mock
  private UserPermissionsService userPermissionsService;

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private UserPermissionsUpdaterService userPermissionsUpdaterService;

  @Nested
  class Update {

    @Test
    public void shouldThrowNullPointerExceptionWhenPermissionsIsNull() {
      // given
      Set<Permission> permissions = null;

      // when
      Throwable throwable = catchThrowable(() ->
          userPermissionsUpdaterService.update(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("newPermissions cannot be null");
    }

    @Nested
    class WhenMakePaymentPermission {

      @Test
      public void shouldThrowAccountNotFoundExceptionWhenAccountNotFound() {
        // given
        Set<Permission> permissions = Collections.singleton(Permission.MAKE_PAYMENTS);
        mockSuccessPermissionUpdate();
        given(accountRepository.findById(ANY_ACCOUNT_ID)).willReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() ->
            userPermissionsUpdaterService.update(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions));

        // then
        assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
            .hasMessage("Account was not found.");
      }
    }

    @Nested
    class WhenAccountIsMultiPayer {

      @Test
      public void shouldDoNothingIfAccountAlreadyMultiPayerAccount() {
        // given
        Set<Permission> permissions = Collections.singleton(Permission.MAKE_PAYMENTS);
        mockSuccessPermissionUpdate();
        Account account = mockAccountWithMultiPayerAccount(Boolean.TRUE);

        // when
        userPermissionsUpdaterService.update(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions);

        // then
        verify(account, never()).setMultiPayerAccount(anyBoolean());
      }
    }

    @Nested
    class WhenAccountIsNotMultiPayer {

      @Test
      public void shouldUpdateAccountIfNotMultiPayerAccount() {
        // given
        Set<Permission> permissions = Collections.singleton(Permission.MAKE_PAYMENTS);
        mockSuccessPermissionUpdate();
        Account account = mockAccountWithMultiPayerAccount(Boolean.FALSE);

        // when
        userPermissionsUpdaterService.update(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions);

        // then
        verify(accountRepository, times(1)).updateMultiPayerAccount(account.getId(), Boolean.TRUE);
      }
    }
  }

  private void mockSuccessPermissionUpdate() {
    doNothing().when(userPermissionsService).updatePermissions(any(), any(), anySet());
  }

  private Account mockAccountWithMultiPayerAccount(Boolean isMultiPayerAccount) {

    Account account = mock(Account.class);
    given(account.isMultiPayerAccount()).willReturn(isMultiPayerAccount);
    given(accountRepository.findById(ANY_ACCOUNT_ID)).willReturn(Optional.of(account));
    return account;
  }
}