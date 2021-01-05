package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.AccountUserCode.AccountUserCodeBuilder;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.model.UserEntity.UserEntityBuilder;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.ExpiredUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.InvalidUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.MissingPendingUserIdException;

@ExtendWith(MockitoExtension.class)
class ConfirmEmailChangeServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserService userService;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private TokenToHashConverter tokenToHashConverter;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @InjectMocks
  private ConfirmEmailChangeService confirmEmailChangeService;

  private static final UUID ANY_VALID_TOKEN = UUID
      .fromString("9eb5e907-4414-48e8-8c9d-8ba041a43b38");

  private static final String ANY_VALID_PASSWORD = "p4ssw0rd.12.12";

  @Nested
  class WhenProvidedEmailChangeVerificationTokenIsInvalid {

    @Test
    public void shouldThrowInvalidUserEmailVerificationCodeException() {
      // given
      mockMissingChangeToken();

      // when
      Throwable throwable = catchThrowable(
          () -> confirmEmailChangeService.confirmEmailChange(ANY_VALID_TOKEN, ANY_VALID_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(InvalidUserEmailVerificationCodeException.class)
          .hasMessage("Invalid token");
    }

    private void mockMissingChangeToken() {
      when(tokenToHashConverter.convert(any())).thenReturn("");
      when(accountUserCodeRepository
          .findByCodeAndCodeType(anyString(), any()))
          .thenReturn(Optional.empty());
    }
  }

  @Nested
  class WhenProvidedEmailChangeVerificationTokenIsExpired {

    @Test
    public void shouldThrowExpiredUserEmailVerificationCodeException() {
      // given
      mockExpiredToken();

      // when
      Throwable throwable = catchThrowable(
          () -> confirmEmailChangeService.confirmEmailChange(ANY_VALID_TOKEN, ANY_VALID_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(ExpiredUserEmailVerificationCodeException.class)
          .hasMessage("Expired token");
    }

    private void mockExpiredToken() {
      AccountUserCode code = createBaseAccountUserCode()
          .status(CodeStatus.EXPIRED)
          .expiration(LocalDateTime.now().minusMinutes(1))
          .build();
      mockAccountUserCodeWith(code);
    }
  }

  @Nested
  class WhenUserAssociatedWithTokenDoesNotExist {

    @Test
    public void shouldThrowAccountUserNotFoundException() {
      // given
      mockNotExistingAccountUser();

      // when
      Throwable throwable = catchThrowable(
          () -> confirmEmailChangeService.confirmEmailChange(ANY_VALID_TOKEN, ANY_VALID_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("AccountUser does not exist.");
    }

    private void mockNotExistingAccountUser() {
      AccountUserCode code = createBaseAccountUserCode().build();
      mockAccountUserCodeWith(code);
    }
  }

  @Nested
  class WhenUserAssociatedWithTokenHasNoPendingUserIdAttribute {

    @Test
    public void shouldThrowMissingPendingUserIdException() {
      // given
      mockExistingUserWithoutPendingUserId();

      // when
      Throwable throwable = catchThrowable(
          () -> confirmEmailChangeService.confirmEmailChange(ANY_VALID_TOKEN, ANY_VALID_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(MissingPendingUserIdException.class)
          .hasMessage("User does not expect an email change.");
    }

    private void mockExistingUserWithoutPendingUserId() {
      UUID accountUserId = UUID.fromString("3bfd4c32-8bc7-4a6d-b9db-72878ae0863f");
      AccountUserCode code = createBaseAccountUserCode()
          .accountUserId(accountUserId)
          .build();
      UserEntity user = createBaseUser().id(accountUserId).pendingUserId(null).build();
      mockAccountUserCodeWith(code);
      when(userRepository.findById(any())).thenReturn(Optional.of(user));
      doNothing().when(userService).setPassword((UserEntity) any(), anyString());
    }
  }

  @Nested
  class WhenServiceCallIsSuccessful {

    @Test
    public void shouldNotReturnAnyError() {
      // given
      UUID accountUserId = UUID.fromString("3bfd4c32-8bc7-4a6d-b9db-72878ae0863f");
      AccountUserCode code = createBaseAccountUserCode()
          .accountUserId(accountUserId)
          .build();
      UserEntity user = createBaseUser().id(accountUserId).build();
      mockAccountUserCodeWith(code);
      when(userRepository.findById(any())).thenReturn(Optional.of(user));
      when(identityProvider.getEmailByIdentityProviderId(any())).thenReturn("user@email.com");
      doNothing().when(identityProvider).deleteUser(anyString());
      doNothing().when(userService).setPassword((UserEntity) any(), anyString());

      // when
      confirmEmailChangeService.confirmEmailChange(ANY_VALID_TOKEN, ANY_VALID_PASSWORD);

      // then
      verify(userService, times(2)).setPassword((UserEntity) any(), anyString());
      verify(identityProvider).verifyEmail((UserEntity) any());
      assertThat(code.getStatus()).isEqualTo(CodeStatus.USED);
    }
  }

  private UserEntityBuilder createBaseUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .email("user@email.com")
        .identityProviderUserId(UUID.randomUUID())
        .isOwner(true)
        .emailVerified(false)
        .pendingUserId(UUID.randomUUID());
  }

  private AccountUserCodeBuilder createBaseAccountUserCode() {
    return AccountUserCode.builder()
        .accountUserId(UUID.randomUUID())
        .code("random-code")
        .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .expiration(LocalDateTime.now().plusHours(1));
  }

  private void mockAccountUserCodeWith(AccountUserCode code) {
    when(tokenToHashConverter.convert(any())).thenReturn("someString");
    when(accountUserCodeRepository.findByCodeAndCodeType(anyString(), any()))
        .thenReturn(Optional.of(code));
  }
}