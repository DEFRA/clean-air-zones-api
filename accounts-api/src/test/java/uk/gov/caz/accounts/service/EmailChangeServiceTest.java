package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

  private static final String ANY_NEW_EMAIL = "a@b.com";
  private static final URI ANY_ROOT_URI = URI.create("http://localhost");

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private EmailChangeService emailChangeService;

  @Nested
  class DiscardEmailChange {

    @Test
    public void shouldDiscardEmailChange() {
      // given
      UserEntity user = UserEntity.builder()
          .id(UUID.randomUUID())
          .identityProviderUserId(UUID.fromString("0f594810-e8f2-4f6d-9461-61eb7f01d701"))
          .pendingUserId(UUID.randomUUID())
          .build();
      mockDeleteUserInExternalProvider(user);

      // when
      emailChangeService.discardEmailChange(user);

      // then
      verify(userRepository).clearPendingUserId(user.getId());
      verify(identityProvider).deleteUser(any());
    }

    private void mockDeleteUserInExternalProvider(UserEntity user) {
      String email = "test@email.com";
      when(identityProvider.getEmailByIdentityProviderId(user.getPendingUserId().get()))
          .thenReturn(email);
      doNothing().when(identityProvider).deleteUser(email);
    }
  }

  @Nested
  class Preconditions {

    @Nested
    class WhenUserExistsWithTheSameEmailAddressAndItsNotUsersPendingUser {

      @Test
      public void shouldThrowNotUniqueEmailException() {
        // given
        UUID existingUserId = UUID.fromString("0f594810-e8f2-4f6d-9461-61eb7f01d701");
        mockExistingUserWithTheNewEmail(ANY_NEW_EMAIL);

        // when
        Throwable throwable = catchThrowable(() -> emailChangeService.initiateEmailChange(
            existingUserId, ANY_NEW_EMAIL, ANY_ROOT_URI));

        // then
        assertThat(throwable).isInstanceOf(NotUniqueEmailException.class);
      }

      private void mockExistingUserWithTheNewEmail(String email) {
        UserEntity existingUser = mock(UserEntity.class);
        when(existingUser.isRemoved()).thenReturn(false);
        when(existingUser.getPendingUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(userRepository.findById(any())).thenReturn(Optional.of(existingUser));
        when(identityProvider.checkIfUserExists(email)).thenReturn(true);
        when(identityProvider.getEmailByIdentityProviderId(any())).thenReturn("other@user.com");
      }
    }

    @Nested
    class WhenAccountUserIdPointsToExistingUser {

      @Nested
      class WhoHasTheSameEmailAddressAsTheRequestedOne {

        @Test
        public void shouldThrowNotUniqueEmailException() {
          // given
          UUID existingUserId = UUID.fromString("0f594810-e8f2-4f6d-9461-61eb7f01d701");
          mockExistingUserWithTheSameEmail(existingUserId);

          // when
          Throwable throwable = catchThrowable(() -> emailChangeService.initiateEmailChange(
              existingUserId, ANY_NEW_EMAIL, ANY_ROOT_URI));

          // then
          assertThat(throwable).isInstanceOf(NotUniqueEmailException.class);
        }

        private void mockExistingUserWithTheSameEmail(UUID existingUserId) {
          UserEntity existingUser = mock(UserEntity.class);
          when(existingUser.isRemoved()).thenReturn(false);
          when(userRepository.findById(existingUserId)).thenReturn(Optional.of(existingUser));
          when(identityProvider.checkIfUserExists(ANY_NEW_EMAIL)).thenReturn(true);
        }
      }
    }

    @Nested
    class WhenAccountUserIdPointsToRemovedUser {

      @Test
      public void shouldThrowAccountUserNotFoundException() {
        // given
        UUID removedUserId = UUID.fromString("0f594810-e8f2-4f6d-9461-61eb7f01d701");
        mockRemovedUser(removedUserId);

        // when
        Throwable throwable = catchThrowable(() -> emailChangeService.initiateEmailChange(
            removedUserId, ANY_NEW_EMAIL, ANY_ROOT_URI));

        // then
        assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class);
      }

      private void mockRemovedUser(UUID nonExistingUserId) {
        UserEntity removedUser = mock(UserEntity.class);
        when(removedUser.isRemoved()).thenReturn(true);
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.of(removedUser));
      }
    }

    @Nested
    class WhenAccountUserIdPointsToNonExistingUser {

      @Test
      public void shouldThrowAccountUserNotFoundException() {
        // given
        UUID nonExistingUserId = UUID.fromString("0f594810-e8f2-4f6d-9461-61eb7f01d701");
        mockNonExistingUser(nonExistingUserId);

        // when
        Throwable throwable = catchThrowable(() -> emailChangeService.initiateEmailChange(
            nonExistingUserId, ANY_NEW_EMAIL, ANY_ROOT_URI));

        // then
        assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class);
      }

      private void mockNonExistingUser(UUID nonExistingUserId) {
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());
      }
    }
  }
}