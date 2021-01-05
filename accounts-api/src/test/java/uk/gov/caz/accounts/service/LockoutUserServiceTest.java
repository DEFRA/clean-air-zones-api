package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.repository.IdentityProvider;

@ExtendWith(MockitoExtension.class)
class LockoutUserServiceTest {

  private static final String SOME_EMAIL = "test@gov.uk";

  @Mock
  private IdentityProvider identityProvider;

  private LockoutUserService lockoutUserService;

  @BeforeEach
  public void initialize() {
    lockoutUserService = new LockoutUserService(identityProvider, 5, 30);
  }

  @Nested
  class LockoutUserIfApplicable {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void shouldIncreaseFailedLoginsByOneAndReturnMessage(int currentFailedLogins) {
      // given
      given(identityProvider.checkIfUserExists(any())).willReturn(true);
      given(identityProvider.getCurrentFailedLogins(any())).willReturn(currentFailedLogins);
      given(identityProvider.getCurrentLockoutTime(any())).willReturn(Optional.empty());
      // when
      Throwable throwable = catchThrowable(
          () -> lockoutUserService.lockoutUserIfApplicable(SOME_EMAIL));
      // then
      shouldReturnIncorrectPasswordOrUsernameMessage(throwable);
      verify(identityProvider).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 88, 99})
    void shouldSetLockoutTimeWhenLockoutTimeIsNotSet(
        int currentFailedLogins) {
      // given
      given(identityProvider.checkIfUserExists(any())).willReturn(true);
      given(identityProvider.getCurrentFailedLogins(any())).willReturn(currentFailedLogins);
      given(identityProvider.getCurrentLockoutTime(any())).willReturn(Optional.empty());
      // when
      Throwable throwable = catchThrowable(
          () -> lockoutUserService.lockoutUserIfApplicable(SOME_EMAIL));
      // then
      shouldReturnIncorrectPasswordOrUsernameMessage(throwable);
      verify(identityProvider).setLockoutTime(SOME_EMAIL);
      verify(identityProvider).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
    }

    @Test
    void shouldResetLockoutTimeAndIncreaseFailedLoginsByOne() {
      // given
      LocalDateTime currentLockoutTimeout = LocalDateTime.now().minusMinutes(45);
      given(identityProvider.checkIfUserExists(any())).willReturn(true);
      given(identityProvider.getCurrentFailedLogins(any())).willReturn(0);
      given(identityProvider.getCurrentLockoutTime(any()))
          .willReturn(Optional.of(currentLockoutTimeout));
      // when
      Throwable throwable = catchThrowable(
          () -> lockoutUserService.lockoutUserIfApplicable(SOME_EMAIL));
      // then
      shouldReturnIncorrectPasswordOrUsernameMessage(throwable);
      verify(identityProvider).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }

    @Test
    void shouldDoNothingWhenUserDoesNotExist() {
      // given
      given(identityProvider.checkIfUserExists(any())).willReturn(false);
      // when
      lockoutUserService.lockoutUserIfApplicable(SOME_EMAIL);
      // then
      verify(identityProvider, never()).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }
  }

  @Nested
  class CheckIfUserIsLockedByLockoutTime {

    @Test
    void shouldThrowUserLockedExceptionWhenUserIsLockedByTimeout() {
      // given
      LocalDateTime currentLockoutTimeout = LocalDateTime.now().minusMinutes(20);
      given(identityProvider.checkIfUserExists(any())).willReturn(true);
      given(identityProvider.getCurrentLockoutTime(any()))
          .willReturn(Optional.of(currentLockoutTimeout));
      // when
      Throwable throwable = catchThrowable(
          () -> lockoutUserService.checkIfUserIsLockedByLockoutTime(SOME_EMAIL));
      // then
      shouldReturnIncorrectPasswordOrUsernameMessage(throwable);
      verify(identityProvider).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }

    @Test
    void shouldDoNothingWhenUserIsNotLockedByTimeout() {
      // given
      LocalDateTime currentLockoutTimeout = LocalDateTime.now().minusMinutes(31);
      given(identityProvider.checkIfUserExists(any())).willReturn(true);
      given(identityProvider.getCurrentLockoutTime(any()))
          .willReturn(Optional.of(currentLockoutTimeout));
      // when
      lockoutUserService.checkIfUserIsLockedByLockoutTime(SOME_EMAIL);
      // then
      verify(identityProvider).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }

    @Test
    void shouldDoNothingWhenUserDoesNotExist() {
      // given
      given(identityProvider.checkIfUserExists(any())).willReturn(false);
      // when
      lockoutUserService.checkIfUserIsLockedByLockoutTime(SOME_EMAIL);
      // then
      verify(identityProvider, never()).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }
  }

  private void shouldReturnIncorrectPasswordOrUsernameMessage(Throwable throwable) {
    assertThat(throwable).hasMessage("Incorrect password or username");
  }

  @Nested
  class UnlockUser {

    @Test
    void shouldUnlockUser() {
      // given

      // when
      lockoutUserService.unlockUser(SOME_EMAIL);
      // then
      verify(identityProvider).resetFailedLoginsAndLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentLockoutTime(SOME_EMAIL);
      verify(identityProvider, never()).increaseFailedLoginsByOne(SOME_EMAIL);
      verify(identityProvider, never()).getCurrentFailedLogins(SOME_EMAIL);
      verify(identityProvider, never()).setLockoutTime(SOME_EMAIL);
    }
  }

  @Test
  void shouldThrownIllegalArgumentExceptionWhenMaximumAllowedFailedLoginsIsEqualsToZero() {
    // given

    // when
    Throwable throwable = catchThrowable(
        () -> new LockoutUserService(identityProvider, 0, 30));
    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }
}