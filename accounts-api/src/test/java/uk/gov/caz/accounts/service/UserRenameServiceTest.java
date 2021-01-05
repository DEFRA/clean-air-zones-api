package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

@ExtendWith(MockitoExtension.class)
class UserRenameServiceTest {

  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Mock
  private UserRepository userRepository;

  @Mock
  private IdentityProvider identityProvider;

  @InjectMocks
  private UserRenameService userRenameService;



  @Test
  public void shouldCallIdentityProviderIfUserIsFound() {
    // given
    given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
        .willReturn(Optional.of(UserEntity.builder()
            .isOwner(false)
            .name("old name")
            .identityProviderUserId(UUID.randomUUID())
            .build()));

    // when
    userRenameService.updateUserName(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, "new name");

    // then
    verify(identityProvider).setUserName(any(), any());

  }

  @Nested
  class ShouldRaiseUserNotFound {

    private void errorNotFoundRaised() {
      Throwable throwable = catchThrowable(() ->
          userRenameService.updateUserName(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, "name"));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("User not found");
      verifyNoInteractions(identityProvider);
    }

    @Test
    public void whenUserIsMissingInDatabase() {
      given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
          .willReturn(Optional.empty());

      errorNotFoundRaised();
    }

    @Test
    public void whenUserIsRemoved() {
      given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
          .willReturn(Optional.of(UserEntity.builder()
              .identityProviderUserId(null)
              .build()));

      errorNotFoundRaised();
    }

    @Test
    public void whenUserIsOwner() {
      given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
          .willReturn(Optional.of(UserEntity.builder()
              .identityProviderUserId(UUID.randomUUID())
              .isOwner(true)
              .build()));

      errorNotFoundRaised();
    }
  }

  @Nested
  class RenameFailsWhen {

    @Test
    public void usernameIsNull() {
      // when
      Throwable throwable = catchThrowable(() ->
          userRenameService.updateUserName(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, null));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void usernameIsEmpty() {
      // when
      Throwable throwable = catchThrowable(() ->
          userRenameService.updateUserName(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, ""));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }

}