package uk.gov.caz.accounts.service;

import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.service.exception.InvalidAccountUserPasswordResetCodeException;

/**
 * Service responsible for updating password in the password reset process. It validates provided
 * token and if it is valid then it updates the new password. After successful process token is
 * marked as used and cannot be reused.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SetPasswordService {

  private final UserCodeService userCodeService;
  private final UserService userService;
  private final VerificationEmailConfirmationService verificationEmailConfirmationService;
  private final LockoutUserService lockoutUserService;

  /**
   * Method which validates provided token and updates password in Identity Provider. If user's
   * email was not verified already this method will verify it.
   *
   * @param token provided token.
   * @param password new password.
   */
  @Transactional
  public void process(UUID token, String password) {
    checkPreconditions(token, password);

    if (!userCodeService.isActive(token, CodeType.PASSWORD_RESET)) {
      throw new InvalidAccountUserPasswordResetCodeException();
    }
    User user = userCodeService.findUserByTokenAndCodeType(token, CodeType.PASSWORD_RESET);
    userCodeService.markCodeAsUsed(token);
    userService.setPassword(user, password);

    lockoutUserService.unlockUser(user.getEmail());

    if (!user.isEmailVerified()) {
      log.info("Setting new password flow: User's email was not yet verified. Verifying email: {}",
          mask(user.getEmail()));
      verificationEmailConfirmationService.verifyUser(user);
    }
  }

  /**
   * Checks if provided details are valid.
   *
   * @param token provided token
   * @param password provided password
   */
  private void checkPreconditions(UUID token, String password) {
    Preconditions.checkNotNull(token, "Token cannot be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(password),
        "Password cannot be null or empty");
  }
}
