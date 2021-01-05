package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.exception.AccountUserCodeHashCreationException;
import uk.gov.caz.accounts.service.exception.AccountUserCodeNotFoundException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserCodeServiceTest {

  public static final AccountUserCode ACCOUNT_USER_CODE = AccountUserCode.builder()
      .id(1)
      .accountUserId(UUID.randomUUID())
      .status(CodeStatus.ACTIVE)
      .expiration(LocalDateTime.now().plusDays(1))
      .code("exampleCode")
      .codeType(CodeType.PASSWORD_RESET)
      .build();

  public static final User NOT_DELETED_USER = User.builder().identityProviderUserId(UUID.randomUUID()).build();

  public static final User DELETED_USER = User.builder().build();

  @Mock
  AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  AccountUserRepository accountUserRepository;

  @Mock
  IdentityProvider identityProvider;

  @Mock
  TokenToHashConverter tokenToHashConverter;

  @InjectMocks
  UserCodeService userCodeService;

  @Nested
  class Validate {

    @Test
    void shouldReturnFalseWhenCodeNotFound() {
      // given
      mockRepositoryNotFoundCode();

      // when
      boolean result = userCodeService.isActive(any(), CodeType.PASSWORD_RESET);

      // then
      assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenUserIsDeleted() {
      // given
      UUID token = UUID.randomUUID();
      mockRepositoryFoundActiveCode();
      mockRepositoryFoundUser(DELETED_USER);
      mockSuccessTokenHashing(token);

      // when
      boolean result = userCodeService.isActive(token, CodeType.PASSWORD_RESET);

      // then
      assertThat(result).isFalse();
    }


    @Test
    void shouldThrowAccountUserCodeIsNotValidExceptionWhenFoundCodeIsNotActive() {
      // given
      mockRepositoryFoundInactiveCode();

      // when
      boolean result = userCodeService.isActive(any(), CodeType.PASSWORD_RESET);

      // then
      assertThat(result).isFalse();
    }

    @Test
    void shouldNotThrowExceptionIfFoundCodeIsActive() {
      // given
      UUID token = UUID.randomUUID();
      mockRepositoryFoundActiveCode();
      mockRepositoryFoundUser(NOT_DELETED_USER);
      mockSuccessTokenHashing(token);

      // when
      Throwable throwable = catchThrowable(
          () -> userCodeService.isActive(token, CodeType.PASSWORD_RESET));

      // then
      assertThat(throwable).isEqualTo(null);
    }

    @Test
    void shouldThrowExceptionIfUserIsNotFound() {
      // given
      UUID token = UUID.randomUUID();
      mockRepositoryFoundActiveCode();
      given(accountUserRepository.findById(ACCOUNT_USER_CODE.getAccountUserId()))
          .willReturn(Optional.empty());
      mockSuccessTokenHashing(token);

      // when
      Throwable throwable = catchThrowable(
          () -> userCodeService.isActive(token, CodeType.PASSWORD_RESET));

      // then
      assertThat(throwable).isInstanceOf(UserNotFoundException.class)
          .hasMessage("Token is invalid or expired");
    }

    private void mockRepositoryFoundUser(User user) {
      given(accountUserRepository.findById(ACCOUNT_USER_CODE.getAccountUserId()))
          .willReturn(Optional.of(user));
    }

    private void mockRepositoryNotFoundCode() {
      given(accountUserCodeRepository.findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
          .willReturn(Optional.empty());
    }

    private void mockRepositoryFoundInactiveCode() {
      AccountUserCode accountUserCode = AccountUserCode.builder()
          .id(1)
          .accountUserId(UUID.randomUUID())
          .status(CodeStatus.EXPIRED)
          .expiration(LocalDateTime.now().minusDays(1))
          .code("exampleCode")
          .codeType(CodeType.PASSWORD_RESET)
          .build();
      given(accountUserCodeRepository.findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
          .willReturn(Optional.of(accountUserCode));
    }

    private void mockRepositoryFoundActiveCode() {
      given(accountUserCodeRepository.findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
          .willReturn(Optional.of(ACCOUNT_USER_CODE));
    }
  }

  @Nested
  class CalcHashOf {

    @Test
    void shouldThrowExceptionIfNotAbleToConvertTokenToHash() {
      // given
      mockFailedTokenHashing();

      // when
      Throwable throwable = catchThrowable(() -> userCodeService.isActive(UUID.randomUUID(),
          CodeType.PASSWORD_RESET));

      // then
      assertThat(throwable).isInstanceOf(AccountUserCodeHashCreationException.class);
      assertThat(throwable).hasMessage("Error Message");
    }

    private void mockFailedTokenHashing() {
      given(tokenToHashConverter.convert(any()))
          .willThrow(new AccountUserCodeHashCreationException("Error Message"));
    }
  }

  @Nested
  class MarkCodeAsUsed {

    @Test
    void shouldUpdateCodeStatusInDB() {
      //given
      UUID token = UUID.randomUUID();
      mockSuccessTokenHashing(token);

      //when
      userCodeService.markCodeAsUsed(token);

      //then
      verify(tokenToHashConverter).convert(token);
      verify(accountUserCodeRepository).setStatusForCode(anyString(), any(CodeStatus.class));
    }
  }

  @Nested
  class FindUserByToken {

    private static final String USER_EMAIL = "somemail@gmail.com";

    @Test
    void shouldThrowExceptionIfNotAbleToFindAccountUserCode() {
      // given
      UUID token = UUID.randomUUID();
      mockSuccessTokenHashing(token);
      mockAccountUserCodeNotFound();

      // when
      Throwable throwable = catchThrowable(() -> userCodeService.findUserByTokenAndCodeType(token,
          CodeType.PASSWORD_RESET));

      // then
      assertThat(throwable).isInstanceOf(AccountUserCodeNotFoundException.class);
      assertThat(throwable).hasMessage("AccountUserCode not found.");
      verify(accountUserRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionIfNotAbleToFindUserInDb() {
      // given
      UUID token = UUID.randomUUID();
      mockSuccessTokenHashing(token);
      mockAccountUserCodeFound();
      mockUserNotFoundInDb();

      // when
      Throwable throwable = catchThrowable(() -> userCodeService.findUserByTokenAndCodeType(token,
          CodeType.PASSWORD_RESET));

      // then
      assertThat(throwable).isInstanceOf(UserNotFoundException.class);
      assertThat(throwable).hasMessage("Cannot process request.");
    }

    @Test
    void shouldThrowExceptionIfNotAbleToFindUserInIdentityProvider() {
      // given
      UUID token = UUID.randomUUID();
      mockSuccessTokenHashing(token);
      mockAccountUserCodeFound();
      mockUserFoundInDb();
      mockUserNotFoundInIdentityProvider();

      // when
      Throwable throwable = catchThrowable(() -> userCodeService.findUserByTokenAndCodeType(token,
          CodeType.PASSWORD_RESET));

      // then
      // User found in our DB but not in IdentityProvider should result in 500 and serious
      // internal error
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    void shouldReturnUserIfFoundAndTheUserShallCombinePropertiesFromDbAndIdentityProvider() {
      // given
      UUID token = UUID.randomUUID();
      mockSuccessTokenHashing(token);
      mockAccountUserCodeFound();
      mockUserFoundInDb();
      mockUserFoundInIdentityProvider();

      // when
      User user = userCodeService.findUserByTokenAndCodeType(token, CodeType.PASSWORD_RESET);

      // then
      verify(accountUserCodeRepository).findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET));
      verify(accountUserRepository).findById(any());
      assertThat(user).isNotNull();
      assertThat(user.getEmail()).isEqualTo(USER_EMAIL);
      assertThat(user.isEmailVerified()).isTrue();
      assertThat(user.getAccountId()).isNotNull();
    }

    private void mockAccountUserCodeNotFound() {
      given(accountUserCodeRepository.findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
          .willReturn(Optional.empty());
    }

    private void mockAccountUserCodeFound() {
      AccountUserCode accountUserCode = AccountUserCode.builder()
          .id(1)
          .accountUserId(UUID.randomUUID())
          .status(CodeStatus.EXPIRED)
          .expiration(LocalDateTime.now().minusDays(1))
          .code("exampleCode")
          .codeType(CodeType.PASSWORD_RESET)
          .build();
      given(accountUserCodeRepository.findByCodeAndCodeType(any(), eq(CodeType.PASSWORD_RESET)))
          .willReturn(Optional.of(accountUserCode));
    }

    private void mockUserNotFoundInDb() {
      given(accountUserRepository.findById(any())).willReturn(Optional.empty());
    }

    private void mockUserFoundInDb() {
      User user = User.builder()
          .accountId(UUID.randomUUID())
          .id(UUID.randomUUID())
          .build();
      given(accountUserRepository.findById(any())).willReturn(Optional.of(user));
    }

    private void mockUserNotFoundInIdentityProvider() {
      given(identityProvider.getEmailByIdentityProviderId(any()))
          .willThrow(new IdentityProviderUnavailableException());
    }

    private void mockUserFoundInIdentityProvider() {
      User user = User.builder()
          .email(USER_EMAIL)
          .emailVerified(true)
          .build();
      given(identityProvider.getEmailByIdentityProviderId(any())).willReturn(USER_EMAIL);
      given(identityProvider.getUser(USER_EMAIL)).willReturn(user);
    }
  }

  private void mockSuccessTokenHashing(UUID token) {
    given(tokenToHashConverter.convert(token))
        .willReturn("ExampleCode");
  }
}