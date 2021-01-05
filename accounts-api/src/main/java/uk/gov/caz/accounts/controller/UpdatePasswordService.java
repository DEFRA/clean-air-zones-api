package uk.gov.caz.accounts.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.service.RecentlyUsedPasswordChecker;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.exception.OldPasswordWrongException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;
import uk.gov.caz.accounts.util.Strings;

/**
 * Service used to update password for an existing account.
 */
@Service
@Slf4j
@AllArgsConstructor
public class UpdatePasswordService {

  private final UserService userService;

  private final IdentityProvider identityProvider;

  private final RecentlyUsedPasswordChecker recentlyUsedPasswordChecker;

  /**
   * Processes request to update a password.
   * @param accountUserId account to which password is being updated
   * @param oldPassword old password for this account
   * @param newPassword new password user wishes to change it to
   */
  public void process(UUID accountUserId, String oldPassword, String newPassword) {
    log.info("Updating password for accountUserId {}", accountUserId);
    UserEntity user = userService.findUser(accountUserId)
        .orElseThrow(() -> new UserNotFoundException(String.format(
            "User for accountUserId %s not found", accountUserId)));
    checkIfOldPasswordIsValid(user, oldPassword);
    recentlyUsedPasswordChecker.checkIfPasswordWasNotUsedRecently(newPassword, user.getEmail());
    userService.setPassword(user, newPassword);
    log.info("Password changed for user");
  }

  /**
   * Checks if old password for this account is valid.
   * @param user user having the password
   * @param oldPassword old password being checked
   */
  private void checkIfOldPasswordIsValid(UserEntity user, String oldPassword) {
    String email = identityProvider.getEmailByIdentityProviderId(user.getIdentityProviderUserId());
    String maskedEmail = Strings.mask(email);
    log.debug("Found email for user : {}", maskedEmail);
    try {
      identityProvider.loginUser(user.getEmail(), oldPassword);
    } catch (NotAuthorizedException exception) {
      logAwsExceptionDetails(maskedEmail, exception);
      throw new OldPasswordWrongException("The password you entered is wrong for thi user");
    }
  }

  /**
   * Logs details of the {@code exception} alongside the email address.
   */
  private void logAwsExceptionDetails(String maskedEmail,
      CognitoIdentityProviderException exception) {
    AwsErrorDetails details = exception.awsErrorDetails();
    log.warn("Unable to log in user: '{}', error code: {}, error message: {}", maskedEmail,
        details.errorCode(), details.errorMessage());
  }
}
