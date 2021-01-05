package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class VerificationEmailConfirmationServiceTest {

  @InjectMocks
  private VerificationEmailConfirmationService verificationEmailConfirmationService;

  @Mock
  private UserCodeService userCodeService;

  @Mock
  private AccountUserRepository accountUserRepository;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserRemovalService userRemovalService;

  private String ANY_EMAIL = "email@dev.co.uk";

  @Test
  public void shouldCallIdentityProviderIfUserHasAnEmail() {
    // given
    User user = User.builder().email(ANY_EMAIL).build();

    // when
    verificationEmailConfirmationService.verifyUser(user);

    // then
    verify(identityProvider).verifyEmail(eq(user));
  }


  @Test
  public void shouldNotAllowToVerifyUserWithoutAnEmail() {
    // given
    User userWithoutEmail = User.builder().build();

    // when
    Throwable throwable = catchThrowable(
        () -> verificationEmailConfirmationService.verifyUser(userWithoutEmail));

    // then
    assertThat(throwable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User email cannot be empty.");
    verifyNoInteractions(identityProvider);
  }
}