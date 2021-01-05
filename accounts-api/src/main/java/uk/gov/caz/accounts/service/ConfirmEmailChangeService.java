package uk.gov.caz.accounts.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.ExpiredUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.InvalidUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.MissingPendingUserIdException;

/**
 * Manages the final step of the email change process.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ConfirmEmailChangeService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final IdentityProvider identityProvider;
  private final TokenToHashConverter tokenToHashConverter;
  private final AccountUserCodeRepository accountUserCodeRepository;

  /**
   * Method responsible for assigning new user in IdentityProvider service and setting up new
   * password.
   *
   * @param emailChangeVerificationToken token which checks if user can perform email change.
   * @param password new password for the user.
   */
  @Transactional
  public String confirmEmailChange(UUID emailChangeVerificationToken, String password) {
    AccountUserCode code = getValidatedToken(emailChangeVerificationToken);
    UserEntity user = getUserByCode(code);

    // At first we set the new password for the old account so it
    // can has complexity validated by the Cognito
    setNewUserPassword(user, password);

    assignNewIdentityProviderUserToDbUser(user);
    markCodeAsUsed(code);
    setNewUserPassword(user, password);
    verifyNewUsersEmail(user);

    return getUserEmailByIdentityId(user.getIdentityProviderUserId());
  }

  /**
   * Method responsible for validating the provided token and returning it when it's valid.
   *
   * @param emailChangeVerificationToken token which checks if user can perform email change.
   * @return validated {@link AccountUserCode}.
   */
  private AccountUserCode getValidatedToken(UUID emailChangeVerificationToken) {
    AccountUserCode code = accountUserCodeRepository.findByCodeAndCodeType(
        tokenToHashConverter.convert(emailChangeVerificationToken),
        CodeType.EMAIL_CHANGE_VERIFICATION
    ).orElseThrow(() -> new InvalidUserEmailVerificationCodeException());

    if (!code.isActive()) {
      throw new ExpiredUserEmailVerificationCodeException();
    }

    return code;
  }

  /**
   * Method fetches the {@link UserEntity} associated with the provided token.
   *
   * @param code validated token.
   * @return {@link UserEntity} associated with the provided token.
   */
  private UserEntity getUserByCode(AccountUserCode code) {
    return userRepository.findById(code.getAccountUserId())
        .orElseThrow(() -> new AccountUserNotFoundException("AccountUser does not exist."));
  }

  /**
   * Method performs assignment of the awaiting new user in the IdentityProvider and removes the
   * previous one.
   *
   * @param user {@link UserEntity} representing the DB user.
   */
  private void assignNewIdentityProviderUserToDbUser(UserEntity user) {
    UUID pendingUserId = user.getPendingUserId().orElseThrow(
        () -> new MissingPendingUserIdException("User does not expect an email change."));

    String userEmail = getUserEmailByIdentityId(user.getIdentityProviderUserId());
    identityProvider.deleteUser(userEmail);

    user.setIdentityProviderUserId(pendingUserId);
    user.setPendingUserId(null);
  }

  /**
   * Method sets the code status to {@code CodeStatus.USED}.
   */
  private void markCodeAsUsed(AccountUserCode code) {
    code.setStatus(CodeStatus.USED);
  }

  /**
   * Method performs a call to the identity provider to set the new password.
   *
   * @param user {@link UserEntity} whose email and password is going to be changed.
   * @param password the new password.
   */
  private void setNewUserPassword(UserEntity user, String password) {
    userService.setPassword(user, password);
  }

  /**
   * Method updates the email verification status for the user with new email.
   *
   * @param user {@link UserEntity} whose email and password is going to be changed.
   */
  private void verifyNewUsersEmail(UserEntity user) {
    UserEntity fullUser = userService
        .getCompleteUserDetailsAsUserEntityForAccountUserId(user.getId());
    identityProvider.verifyEmail(fullUser);
  }

  /**
   * Fetches user's email from the third party service.
   */
  private String getUserEmailByIdentityId(UUID identityProviderId) {
    return identityProvider.getEmailByIdentityProviderId(identityProviderId);
  }
}
