package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.service.exception.InvalidAccountUserPasswordResetCodeException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class SetPasswordServiceTest {

  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock
  private VerificationEmailConfirmationService verificationEmailConfirmationService;

  @Mock
  private UserService userService;

  @Mock
  private UserCodeService userCodeService;

  @Mock
  private LockoutUserService lockoutUserService;

  @InjectMocks
  private SetPasswordService setPasswordService;

  @Test
  public void shouldThrowNullPointerExceptionWhenTokenIsNull() {
    // given
    UUID token = null;
    String password = "password";

    // when
    Throwable throwable = catchThrowable(
        () -> setPasswordService.process(token, password));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Token cannot be null");
    verify(lockoutUserService, never()).unlockUser(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPasswordIsNull() {
    // given
    UUID token = UUID.randomUUID();
    String password = null;

    // when
    Throwable throwable = catchThrowable(
        () -> setPasswordService.process(token, password));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Password cannot be null or empty");
    verify(lockoutUserService, never()).unlockUser(any());
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenPasswordIsEmpty() {
    // given
    UUID token = UUID.randomUUID();
    String password = "";

    // when
    Throwable throwable = catchThrowable(
        () -> setPasswordService.process(token, password));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Password cannot be null or empty");
    verify(lockoutUserService, never()).unlockUser(any());
  }

  @Test
  public void shouldThrowAccountUserCodeIsNotValidExceptionWhenTokenIsNotValid() {
    // given
    UUID token = UUID.randomUUID();
    String password = "Password";
    mockInvalidToken(token);

    // when
    Throwable throwable = catchThrowable(
        () -> setPasswordService.process(token, password));

    // then
    assertThat(throwable).isInstanceOf(InvalidAccountUserPasswordResetCodeException.class);
    verify(userCodeService, never()).findUserByTokenAndCodeType(token, CodeType.PASSWORD_RESET);
    verify(userService, never()).setPassword(any(User.class), any());
    verify(userCodeService, never()).markCodeAsUsed(any(UUID.class));
    verify(lockoutUserService, never()).unlockUser(any());
  }

  @Test
  public void shouldThrowUserNotFoundExceptionWhenNotAbleToFindUserInIdentityDB() {
    // given
    UUID token = UUID.randomUUID();
    String password = "Password";
    mockValidToken(token);
    mockUserNotFoundInRepository();

    // when
    Throwable throwable = catchThrowable(
        () -> setPasswordService.process(token, password));

    // then
    assertThat(throwable).isInstanceOf(UserNotFoundException.class)
        .hasMessage("Cannot process request.");
    verify(userService, never()).setPassword(any(User.class), any());
    verify(userCodeService, never()).markCodeAsUsed(any(UUID.class));
    verify(lockoutUserService, never()).unlockUser(any());
  }

  @Test
  public void shouldProcessPasswordResetForValidParamsAndVerifyEmailIfNotYetVerified() {
    requestPasswordSetForUserWithEmailVerificationStatusAndVerifyOperations(EmailVerified.No);
    verify(verificationEmailConfirmationService).verifyUser(any());
    verify(lockoutUserService).unlockUser(any());
  }

  @Test
  public void shouldProcessPasswordResetForValidParamsAndSkipEmailVerificationIfAlreadyVerified() {
    requestPasswordSetForUserWithEmailVerificationStatusAndVerifyOperations(EmailVerified.Yes);
    verify(lockoutUserService).unlockUser(any());
    verifyNoMoreInteractions(userService);
  }

  private void requestPasswordSetForUserWithEmailVerificationStatusAndVerifyOperations(
      EmailVerified emailVerified) {
    // given
    UUID token = UUID.randomUUID();
    String password = "Password";
    mockValidToken(token);
    mockUserFoundInRepository(emailVerified);
    mockValidSetPasswordInUserService();
    mockValidCodeStatusUpdate();

    // when
    setPasswordService.process(token, password);

    // then
    verify(userCodeService).isActive(token, CodeType.PASSWORD_RESET);
    verify(userCodeService).findUserByTokenAndCodeType(token, CodeType.PASSWORD_RESET);
    verify(userCodeService).markCodeAsUsed(any(UUID.class));
    verify(userService).setPassword(any(User.class), any());
  }


  private void mockInvalidToken(UUID token) {
    given(userCodeService.isActive(token, CodeType.PASSWORD_RESET)).willReturn(false);
  }

  private void mockValidToken(UUID token) {
    given(userCodeService.isActive(token, CodeType.PASSWORD_RESET)).willReturn(true);
  }

  private void mockUserNotFoundInRepository() {
    given(userCodeService.findUserByTokenAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
        .willThrow(new UserNotFoundException("Cannot process request."));
  }

  private static enum EmailVerified {
    Yes, No
  }

  private void mockUserFoundInRepository(EmailVerified emailVerified) {
    User user = User.builder()
        .accountId(ACCOUNT_ID)
        .email("email@test.com")
        .isOwner(true)
        .identityProviderUserId(UUID.randomUUID())
        .id(USER_ID)
        .emailVerified(emailVerified == EmailVerified.Yes ? true : false)
        .build();
    given(userCodeService.findUserByTokenAndCodeType(any(), eq(CodeType.PASSWORD_RESET))).willReturn(user);
  }

  private void mockValidSetPasswordInUserService() {
    doNothing().when(userService).setPassword(any(User.class), any());
  }

  private void mockValidCodeStatusUpdate() {
    doNothing().when(userCodeService).markCodeAsUsed(any(UUID.class));
  }
}