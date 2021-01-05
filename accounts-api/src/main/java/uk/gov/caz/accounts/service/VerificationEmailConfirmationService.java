package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserCodeNotFoundException;
import uk.gov.caz.accounts.service.exception.EmailAlreadyVerifiedException;
import uk.gov.caz.accounts.service.exception.ExpiredUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.InvalidUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;

@Service
@AllArgsConstructor
@Slf4j
public class VerificationEmailConfirmationService {

  private final UserCodeService userCodeService;
  private final IdentityProvider identityProvider;
  private final UserRepository userRepository;
  private final UserRemovalService userRemovalService;

  /**
   * Verifies the account associated with the passed {@code token}.
   *
   * @param token A token (account user code) generated and sent to the user.
   */
  @Transactional
  public void verifyUserEmail(UUID token) {
    User user = findUserByToken(token);

    // invariant: user is not null, token exists

    if (user.isEmailVerified()) {
      throw new EmailAlreadyVerifiedException();
    }

    if (!userCodeService.isActive(token, CodeType.USER_VERIFICATION)) {
      throw new ExpiredUserEmailVerificationCodeException();
    }

    userCodeService.markCodeAsUsed(token);
    verifyEmailInIdentityProviderFor(user);
  }

  /**
   * Verifies the user in identity provider.
   *
   * @param user An user being verified.
   */
  @Transactional
  public void verifyUser(User user) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(user.getEmail()),
        "User email cannot be empty.");
    verifyEmailInIdentityProviderFor(user);
  }

  /**
   * Finds the user by the passed user-verification token.
   *
   * @throws InvalidUserEmailVerificationCodeException if token is not found or it is not
   *     associated to any user
   **/
  private User findUserByToken(UUID token) {
    try {
      return userCodeService.findUserByTokenAndCodeType(token, CodeType.USER_VERIFICATION);
    } catch (AccountUserCodeNotFoundException | UserNotFoundException e) {
      log.info("Invalid token detected: ", e);
      throw new InvalidUserEmailVerificationCodeException();
    }
  }

  private void verifyEmailInIdentityProviderFor(User userWithEmail) {
    identityProvider.verifyEmail(userWithEmail);
  }
}
