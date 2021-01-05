package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.AccountUserPermissionRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;

@ExtendWith(MockitoExtension.class)
class DuplicatedAccountUserServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountUserPermissionRepository accountUserPermissionRepository;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  private IdentityProvider identityProvider;

  @InjectMocks
  private DuplicatedAccountUserService duplicatedAccountUserService;

  private static final String ANY_EMAIL = "sample@email.com";

  @Nested
  class InputValidation {

    @Test
    public void shouldThrowNullPointerExceptionWhenEmailIsNull() {
      // given
      String email = null;

      // when
      Throwable throwable = catchThrowable(
          () -> duplicatedAccountUserService.resolveAccountUserDuplication(email));

      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("email cannot be null");
    }

    @Test
    public void shouldThrowAccountUserNotFoundExceptionWhenAccountIsNotFound() {
      // given
      when(userService.getUserEntityByEmail(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(
          () -> duplicatedAccountUserService.resolveAccountUserDuplication(ANY_EMAIL));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("User does not exist");
    }
  }

  @Test
  public void shouldThrowAccountUserNotExpiredExceptionWhenUserHasActiveOrPendingCodes() {
    // given
    mockExistingUserCodes();

    // when
    Throwable throwable = catchThrowable(() ->
        duplicatedAccountUserService.resolveAccountUserDuplication(ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(NotUniqueEmailException.class)
        .hasMessage("User with given email already exists.");
  }

  @Test
  public void shouldThrowNotUniqueEmailExceptionIfUserHasEmailVerified() {
    // given
    when(userService.getUserEntityByEmail(any())).thenReturn(Optional.of(getVerifiedUser()));

    // when
    Throwable throwable = catchThrowable(() ->
        duplicatedAccountUserService.resolveAccountUserDuplication(ANY_EMAIL));

    // then
    assertThat(throwable).isInstanceOf(NotUniqueEmailException.class)
        .hasMessage("User with given email already exists.");
  }

  @Test
  public void shouldDeleteAccountUserAndCodesWhenUserHasNoActiveOrPendingCodes() {
    // given
    mockNonExistingUserCodes();

    // when
    duplicatedAccountUserService.resolveAccountUserDuplication(ANY_EMAIL);

    // then
    verify(accountUserCodeRepository).deleteByAccountUserId(any());
    verify(accountUserPermissionRepository).deleteByAccountUserId(any());
    verify(userRepository).delete(any());
  }

  private void mockExistingUserCodes() {
    when(userService.getUserEntityByEmail(any())).thenReturn(Optional.of(getSampleUser()));
    when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeTypeIn(any(), any(), any()))
        .thenReturn(Arrays.asList(sampleAccountUserCode()));
  }

  private void mockNonExistingUserCodes() {
    when(userService.getUserEntityByEmail(any())).thenReturn(Optional.of(getSampleUser()));
    when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeTypeIn(any(), any(), any()))
        .thenReturn(Collections.emptyList());
  }

  private AccountUserCode sampleAccountUserCode() {
    return AccountUserCode.builder()
        .accountUserId(UUID.randomUUID())
        .expiration(LocalDateTime.now().plusDays(1))
        .code("sample-code")
        .codeType(CodeType.USER_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .build();
  }

  private UserEntity getVerifiedUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .email("sample@email.com")
        .identityProviderUserId(UUID.randomUUID())
        .accountId(UUID.randomUUID())
        .isOwner(true)
        .emailVerified(true)
        .build();
  }

  private UserEntity getSampleUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .email("sample@email.com")
        .identityProviderUserId(UUID.randomUUID())
        .accountId(UUID.randomUUID())
        .isOwner(true)
        .emailVerified(false)
        .build();
  }
}