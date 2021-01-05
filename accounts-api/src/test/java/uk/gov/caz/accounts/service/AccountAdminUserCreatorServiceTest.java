package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountAdminUserCreatorServiceTest {

  private static final String ACCOUNT_ID = "497c84f4-5062-40f2-a306-dd922f08d82c";
  private static final String ACCOUNT_USER_ID = "61cf598c-6564-4c8d-aced-7e2857a64522";
  private static final String ACCOUNT_NAME = "account name";

  private static final String EMAIL = "dev@jaqu.gov";
  private static final String PASSWORD = "passw00rd";
  private static final String VERIFICATION_URL = "http://example.com";

  @Mock
  private UserService userService;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private VerificationEmailIssuerService verificationEmailIssuerService;

  @Mock
  private TokensExpiryDatesProvider tokensExpiryDatesProvider;

  @Mock
  private DuplicatedAccountUserService duplicatedAccountUserService;

  @Mock
  private UserPermissionsService userPermissionService;

  @Mock
  private PendingOwnersRemovalService pendingOwnersRemovalService;

  @InjectMocks
  private AccountAdminUserCreatorService accountAdminUserCreatorService;

  @Nested
  class InputValidation {

    @Test
    public void shouldThrowNullPointerExceptionWhenAccountIdIsNull() {
      Throwable throwable = catchThrowable(() -> accountAdminUserCreatorService
          .createAdminUserForAccount(null, EMAIL, PASSWORD, URI.create(VERIFICATION_URL)));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("accountId cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenEmailIsNull() {
      Throwable throwable = catchThrowable(() -> accountAdminUserCreatorService
          .createAdminUserForAccount(UUID.fromString(ACCOUNT_ID), null, PASSWORD,
              URI.create(VERIFICATION_URL)));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("email cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenPasswordIsNull() {
      Throwable throwable = catchThrowable(() -> accountAdminUserCreatorService
          .createAdminUserForAccount(UUID.fromString(ACCOUNT_ID), EMAIL, null,
              URI.create(VERIFICATION_URL)));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("password cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenVerificationUriIsNull() {
      Throwable throwable = catchThrowable(() -> accountAdminUserCreatorService
          .createAdminUserForAccount(UUID.fromString(ACCOUNT_ID), EMAIL, PASSWORD, null));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("verificationUri cannot be null");
    }
  }

  @Nested
  class NewUserCreationScenario {

    @Test
    public void shouldThrowAccountNotFoundExceptionWhenAccountDoesNotExist() {
      mockAccountNotFound();

      Throwable throwable = catchThrowable(() -> callCreationService());

      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    @Test
    public void shouldCallEmailIssuerOnSuccessfulUserCreation() {
      // given
      mockSuccessfulAccountUserCreation();

      // when
      callCreationService();

      // then
      verify(verificationEmailIssuerService)
          .generateVerificationTokenAndSendVerificationEmail(any(), any(), any());
    }

    @Test
    public void shouldNotCallDuplicatedAccountUserService() {
      mockSuccessfulAccountUserCreation();
      callCreationService();
      verify(duplicatedAccountUserService, never()).resolveAccountUserDuplication(any());
    }


    @Test
    public void shouldReturnUserAndAccountPairOnSuccessfulUserCreation() {
      mockSuccessfulAccountUserCreation();
      UUID accountId = UUID.fromString(ACCOUNT_ID);
      callCreationService();

      verify(duplicatedAccountUserService, never()).resolveAccountUserDuplication(any());
    }

    private void mockSuccessfulNewUserCreation() {
      UserEntity user = sampleCreatedUser();
      when(userService.getUserByEmail(any())).thenReturn(Optional.empty());
      when(userService.createAdminUser(any(), any(), any())).thenReturn(user);
    }
  }

  @Test
  public void shouldAssignAllPermissionsIfUserCreatedIsTheFirstForHisAccount() {
    // given
    mockSuccessfulAccountUserCreation();

    // when
    callCreationService();

    // then
//    verify(accountUserPermissionRepository).addAllPermissionsForUser(any());
  }

  @Test
  public void shouldNotAssignAllPermissionsIfUserCreatedIsAnotherUserForHisAccount() {
    // given
    mockSuccessfulAccountUserCreation();

    // when
    callCreationService();

    // then
    verifyNoInteractions(userPermissionService);
  }

  @Nested
  class ExistingUserAlterationScenario {

    @Test
    public void shouldThrowAccountNotFoundExceptionWhenAccountDoesNotExist() {
      mockAccountNotFound();

      Throwable throwable = catchThrowable(() -> callCreationService());

      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    @Test
    public void shouldCallEmailIssuerOnSuccessfulUserCreation() {
      // given
      mockSuccessfulAccountUserAlteration();

      // when
      callCreationService();

      // then
      verify(verificationEmailIssuerService)
          .generateVerificationTokenAndSendVerificationEmail(any(), any(), any());
    }

    @Test
    public void shouldResolveAccountUsersDuplication() {
      // given
      mockSuccessfulAccountUserAlteration();

      // when
      callCreationService();

      // then
      verify(duplicatedAccountUserService).resolveAccountUserDuplication(any());
      verify(userService).createAdminUserForExistingEmail(any(), any(), any());
    }
  }

  private void mockSuccessfulAccountUserAlteration() {
    mockFoundAccount();
    mockSuccessfulUserAlteration();
    mockSuccessfulEmailSending();
    mockExpiryDate();
  }

  private void mockSuccessfulUserAlteration() {
    UserEntity user = sampleCreatedUser();
    when(userService.getUserEntityByEmail(any())).thenReturn(Optional.of(user));
    doNothing().when(duplicatedAccountUserService).resolveAccountUserDuplication(any());
    when(userService.createAdminUserForExistingEmail(any(), any(), any())).thenReturn(user);
  }

  private void mockAccountNotFound() {
    when(accountRepository.findById(any())).thenReturn(Optional.empty());
  }

  private void mockFoundAccount() {
    Account account = sampleExistingAccount();
    when(accountRepository.findById(any())).thenReturn(Optional.of(account));
  }

  private void mockSuccessfulEmailSending() {
    doNothing().when(verificationEmailIssuerService)
        .generateVerificationTokenAndSendVerificationEmail(any(), any(), any());
  }

  private void mockExpiryDate() {
    when(tokensExpiryDatesProvider.getVerificationEmailExpiryDateFromNow())
        .thenReturn(LocalDateTime.now().plusDays(1));
  }

  private Account sampleExistingAccount() {
    return Account.builder()
        .name(ACCOUNT_NAME)
        .id(UUID.fromString(ACCOUNT_ID))
        .build();
  }

  private UserEntity sampleCreatedUser() {
    return UserEntity.builder()
        .id(UUID.fromString(ACCOUNT_USER_ID))
        .email(EMAIL)
        .identityProviderUserId(UUID.randomUUID())
        .accountId(UUID.fromString(ACCOUNT_ID))
        .isOwner(true)
        .emailVerified(true)
        .build();
  }

  private void mockSuccessfulAccountUserCreation() {
    mockFoundAccount();
    mockSuccessfulUserCreation();
    mockSuccessfulEmailSending();
    mockExpiryDate();
  }

  private void mockSuccessfulUserCreation() {
    UserEntity user = sampleCreatedUser();
    when(userService.createAdminUser(any(), any(), any())).thenReturn(user);
  }

  private Pair<UserEntity, Account> callCreationService() {
    return accountAdminUserCreatorService
        .createAdminUserForAccount(
            UUID.fromString(ACCOUNT_ID),
            EMAIL,
            PASSWORD,
            URI.create(VERIFICATION_URL)
        );
  }
}
