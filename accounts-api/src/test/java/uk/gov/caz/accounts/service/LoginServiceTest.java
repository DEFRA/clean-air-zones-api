package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.repository.exception.PendingEmailChangeException;
import uk.gov.caz.accounts.service.exception.UserLockoutException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

  private final String ANY_EMAIL = "example@email.com";
  private final String ANY_PASSWORD = "password";
  private final boolean IS_USER_BETA_TESTER = true;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private CleanupActiveCodesService cleanupActiveCodesService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LockoutUserService lockoutUserService;

  @Mock
  private UserService userService;

  @Mock
  private CleanupExpiredEmailChangeProcess cleanupExpiredEmailChangeProcess;

  @InjectMocks
  private LoginService loginService;

  @Test
  void shouldLoginUser() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    String accountName = "some-account";
    Account account = Account.builder()
        .name(accountName)
        .id(UUID.randomUUID())
        .build();
    mockRepositoriesWithFoundAccount(identityProviderUserId, account);

    // when
    LoginData loginData = loginService.login(ANY_EMAIL, ANY_PASSWORD);

    // then
    assertThat(loginData.getUser()).isNotNull();
    assertThat(loginData.isBetaTester()).isEqualTo(IS_USER_BETA_TESTER);
    assertThat(loginData.getAccount()).isEqualTo(account);
    assertThat(loginData.getPasswordUpdateTimestamp())
        .isEqualToIgnoringNanos(LocalDateTime.of(2020, 1, 10, 13, 42, 56));

    verify(identityProvider, times(1)).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository).findByIdentityProviderUserId(identityProviderUserId);
    verify(accountRepository).findById(account.getId());
    verify(cleanupActiveCodesService, times(1))
        .updateExpiredPasswordResetCodesForUser(loginData.getUser());
    verify(lockoutUserService).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  void shouldLoginUserWithExpiredEmailChange() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    String accountName = "some-account";
    Account account = Account.builder()
        .name(accountName)
        .id(UUID.randomUUID())
        .build();
    mockRepositoriesWithFoundAccount(identityProviderUserId, account);
    mockActiveBytExpiredEmailChangeVerificationCodes();

    // when
    LoginData loginData = loginService.login(ANY_EMAIL, ANY_PASSWORD);

    // then
    assertThat(loginData.getUser()).isNotNull();
    assertThat(loginData.getAccount()).isEqualTo(account);
    assertThat(loginData.getPasswordUpdateTimestamp())
        .isEqualToIgnoringNanos(LocalDateTime.of(2020, 1, 10, 13, 42, 56));

    verify(identityProvider, times(1)).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository).findByIdentityProviderUserId(identityProviderUserId);
    verify(accountRepository).findById(account.getId());
    verify(cleanupActiveCodesService, times(1))
        .updateExpiredPasswordResetCodesForUser(loginData.getUser());
    verify(lockoutUserService).unlockUser(ANY_EMAIL);
    verify(cleanupExpiredEmailChangeProcess).cleanupExpiredEmailChangeForUser(loginData.getUser());
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  void shouldNotLoginUserAndThrowExceptionWhenCleanupCodesFails() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    Account account = mockIdentityProviderAndUserRepository(identityProviderUserId);
    mockCleanupActiveCodesServiceFails();

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable)
        .isInstanceOf(RuntimeException.class);

    verify(identityProvider).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository).findByIdentityProviderUserId(identityProviderUserId);
    verify(accountRepository, never()).findById(account.getId());
    verify(cleanupActiveCodesService, times(1))
        .updateExpiredPasswordResetCodesForUser(any(UserEntity.class));
    verify(lockoutUserService, never()).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  void shouldNotLoginUserAndThrowExceptionWhenAlreadyHaveEmailChangeInProgress() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    Account account = mockIdentityProviderAndUserRepository(identityProviderUserId);
    mockActiveEmailChangeVerificationCodes();

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable)
        .isInstanceOf(PendingEmailChangeException.class)
        .hasMessage("Pending email change");

    verify(identityProvider).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository).findByIdentityProviderUserId(identityProviderUserId);
    verify(accountRepository, never()).findById(account.getId());
    verify(cleanupActiveCodesService, never())
        .updateExpiredPasswordResetCodesForUser(any(UserEntity.class));
    verify(lockoutUserService, never()).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenAccountNotFoundForLoggedInUser() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    mockRepositoriesWithNotFoundAccount(identityProviderUserId);

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
    assertThat(throwable).hasMessage("Cannot find an account for an existing user");
    verify(lockoutUserService).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  void shouldThrowNotAuthorizedException() {
    // given
    given(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD))
        .willThrow(InvalidCredentialsException.class);

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable)
        .isInstanceOf(InvalidCredentialsException.class);

    verify(lockoutUserService, never()).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  public void shouldReturn500StatusCodeWhenUserAuthorizedButNotFoundInDB() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    when(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD)).thenReturn(identityProviderUserId);
    when(userRepository.findByIdentityProviderUserId(identityProviderUserId))
        .thenReturn(Optional.empty());

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable).isInstanceOf(UserNotFoundException.class);
    assertThat(throwable).hasMessage("Cannot process request.");
    verify(lockoutUserService, never()).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  public void shouldNotLoginWhenAccountIsLockedByLockoutTime() {
    // given
    UUID identityProviderUserId = UUID.randomUUID();
    Account account = mockIdentityProviderAndUserRepository(identityProviderUserId);
    doThrow(new UserLockoutException("Incorrect password or username"))
        .when(lockoutUserService).unlockUser(ANY_EMAIL);

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable).hasMessage("Incorrect password or username");
    verify(identityProvider).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository).findByIdentityProviderUserId(identityProviderUserId);
    verify(accountRepository, never()).findById(account.getId());
    verify(cleanupActiveCodesService).updateExpiredPasswordResetCodesForUser(any());
    verify(lockoutUserService).unlockUser(ANY_EMAIL);
    verify(lockoutUserService, never()).lockoutUserIfApplicable(ANY_EMAIL);
  }

  @Test
  public void shouldLockAccountWhenLoginUserOperationThrownNotAuthorizedException() {
    // given
    given(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD))
        .willThrow(getStubbedNotAuthorizedException());
    doThrow(new UserLockoutException("Incorrect password or username"))
        .when(lockoutUserService).lockoutUserIfApplicable(ANY_EMAIL);

    // when
    Throwable throwable = catchThrowable(() -> loginService.login(ANY_EMAIL, ANY_PASSWORD));

    // then
    assertThat(throwable).hasMessage("Incorrect password or username");
    verify(lockoutUserService).lockoutUserIfApplicable(ANY_EMAIL);
    verify(identityProvider).loginUser(ANY_EMAIL, ANY_PASSWORD);
    verify(userRepository, never()).findByIdentityProviderUserId(any());
    verify(accountRepository, never()).findById(any());
    verify(cleanupActiveCodesService, never()).updateExpiredPasswordResetCodesForUser(any());
    verify(lockoutUserService, never()).unlockUser(ANY_EMAIL);
  }

  private Account mockIdentityProviderAndUserRepository(UUID identityProviderUserId) {
    String accountName = "some-account";
    Account account = Account.builder()
        .name(accountName)
        .id(UUID.randomUUID())
        .build();
    UserEntity foundUser = UserEntity.builder()
        .email(ANY_EMAIL)
        .identityProviderUserId(identityProviderUserId)
        .accountId(account.getId())
        .build();
    given(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD))
        .willReturn(identityProviderUserId);
    given(userRepository.findByIdentityProviderUserId(identityProviderUserId))
        .willReturn(Optional.of(foundUser));
    return account;
  }

  private void mockCleanupActiveCodesServiceFails() {
    RuntimeException exception = new RuntimeException();
    doThrow(exception).when(cleanupActiveCodesService)
        .updateExpiredPasswordResetCodesForUser(any());
  }

  private void mockRepositoriesWithFoundAccount(UUID identityProviderUserId, Account account) {
    UserEntity foundUser = UserEntity.builder()
        .email(ANY_EMAIL)
        .identityProviderUserId(identityProviderUserId)
        .accountId(account.getId())
        .build();
    given(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD))
        .willReturn(identityProviderUserId);
    given(identityProvider.isUserBetaTester(ANY_EMAIL))
        .willReturn(IS_USER_BETA_TESTER);
    given(userRepository.findByIdentityProviderUserId(identityProviderUserId))
        .willReturn(Optional.of(foundUser));
    given(accountRepository.findById(foundUser.getAccountId()))
        .willReturn(Optional.of(account));
    given(userService.getPasswordUpdateTimestamp(ANY_EMAIL))
        .willReturn(LocalDateTime.of(2020, 01, 10, 13, 42, 56));
    AccountPermission accountPermission = AccountPermission.builder()
        .name(Permission.MAKE_PAYMENTS)
        .description("make payments")
        .build();
    UserEntity userEntity = UserEntity.builder()
        .accountPermissions(Collections.singletonList(accountPermission))
        .build();
    given(userRepository.findByIdAndAccountId(foundUser.getId(), account.getId()))
        .willReturn(Optional.of(userEntity));
  }

  private void mockRepositoriesWithNotFoundAccount(UUID identityProviderUserId) {
    UserEntity foundUser = UserEntity.builder()
        .email(ANY_EMAIL)
        .identityProviderUserId(identityProviderUserId)
        .accountId(UUID.randomUUID())
        .build();
    given(identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD))
        .willReturn(identityProviderUserId);
    given(userRepository.findByIdentityProviderUserId(identityProviderUserId))
        .willReturn(Optional.of(foundUser));
    given(accountRepository.findById(foundUser.getAccountId())).willReturn(Optional.empty());
  }

  private void mockActiveEmailChangeVerificationCodes() {
    AccountUserCode accountUserCode = AccountUserCode.builder()
        .id(1234)
        .accountUserId(UUID.randomUUID())
        .code("anyCode")
        .status(CodeStatus.ACTIVE)
        .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
        .expiration(LocalDateTime.now().plusDays(7))
        .build();

    given(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(any(), any(), any()))
        .willReturn(Collections.singletonList(accountUserCode));
  }

  private void mockActiveBytExpiredEmailChangeVerificationCodes() {
    AccountUserCode accountUserCode = AccountUserCode.builder()
        .id(1234)
        .accountUserId(UUID.randomUUID())
        .code("anyCode")
        .status(CodeStatus.ACTIVE)
        .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
        .expiration(LocalDateTime.now().minusDays(1))
        .build();

    given(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(any(), any(), any()))
        .willReturn(Collections.singletonList(accountUserCode));
  }

  private AwsErrorDetails getStubbedAwsErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("NotAuthorizedException")
        .errorMessage("Username or password was incorrect.")
        .build();
  }

  private NotAuthorizedException getStubbedNotAuthorizedException() {
    AwsErrorDetails details = getStubbedAwsErrorDetails();

    return NotAuthorizedException.builder().awsErrorDetails(details).build();
  }
}