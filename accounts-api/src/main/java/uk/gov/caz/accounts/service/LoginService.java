package uk.gov.caz.accounts.service;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.repository.exception.PendingEmailChangeException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;
import uk.gov.caz.accounts.util.Strings;

/**
 * Service responsible for login User in the app.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

  private final IdentityProvider identityProvider;
  private final AccountUserCodeRepository accountUserCodeRepository;
  private final AccountRepository accountRepository;
  private final CleanupActiveCodesService cleanupActiveCodesService;
  private final UserRepository userRepository;
  private final LockoutUserService lockoutUserService;
  private final UserService userService;
  private final CleanupExpiredEmailChangeProcess cleanupExpiredEmailChangeProcess;

  /**
   * Method which logs in the passed {@link User} based on provided params using IdentityProvider.
   *
   * @param email Provided email of user who wants to login.
   * @param password Provided password of User who wants to login.
   * @return An instance of {@link LoginData} which contains the authenticated {@link User}
   *     alongside the linked {@link Account}.
   */
  @Transactional
  public LoginData login(String email, String password) {
    try {
      lockoutUserService.checkIfUserIsLockedByLockoutTime(email);
      UUID identityProviderUserId = identityProvider.loginUser(email, password);
      UserEntity authenticatedUser = userRepository
          .findByIdentityProviderUserId(identityProviderUserId)
          .map(user -> enhanceWithEmail(user, email))
          .orElseThrow(() -> new UserNotFoundException("Cannot process request."));
      verifyEmailChangeProcess(authenticatedUser);
      cleanupActiveCodesServiceAfterLogin(authenticatedUser);
      lockoutUserService.unlockUser(email);
      registerSuccessfulSignIn(authenticatedUser);
      return LoginData.of(authenticatedUser, getAccount(authenticatedUser),
          userService.getPasswordUpdateTimestamp(email),
          permissionsForUser(authenticatedUser), identityProvider.isUserBetaTester(email));
    } catch (NotAuthorizedException exception) {
      logAwsExceptionDetails(Strings.mask(email), exception);
      lockoutUserService.lockoutUserIfApplicable(email);
      throw new InvalidCredentialsException("Invalid credentials");
    }
  }

  private Account getAccount(UserEntity authenticatedUser) {
    return accountRepository.findById(authenticatedUser.getAccountId())
        .orElseThrow(
            () -> new IllegalStateException("Cannot find an account for an existing user"));
  }

  /**
   * Creates a new {@link User} instance based on {@code authenticatedUserData} with the passed
   * {@code email}.
   */
  private UserEntity enhanceWithEmail(UserEntity authenticatedUserData, String email) {
    return authenticatedUserData.toBuilder()
        .email(email)
        .build();
  }

  private void verifyEmailChangeProcess(UserEntity user) {
    List<AccountUserCode> accountUserCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeType(user.getId(), CodeStatus.ACTIVE,
            CodeType.EMAIL_CHANGE_VERIFICATION);
    if (!accountUserCodes.isEmpty()) {
      List<AccountUserCode> activeAccountUserCodes = accountUserCodes.stream()
          .filter(AccountUserCode::isActive)
          .collect(toList());
      if (activeAccountUserCodes.isEmpty()) {
        cleanupExpiredEmailChangeProcess.cleanupExpiredEmailChangeForUser(user);
      } else {
        throw new PendingEmailChangeException("Pending email change");
      }

    }
  }

  private void cleanupActiveCodesServiceAfterLogin(UserEntity user) {
    try {
      cleanupActiveCodesService.updateExpiredPasswordResetCodesForUser(user);
    } catch (Exception e) {
      log.error("Error while cleaning active codes for user '{}'", user.getId(), e);
      throw e;
    }
  }

  /**
   * Set LAST_SIGN_IN_TIMESTMP for user after successful login.
   */
  private void registerSuccessfulSignIn(UserEntity user) {
    userRepository.setLastSingInTimestamp(user.getId());
  }

  /**
   * Method that does fetch account permissions for given user.
   */
  private List<Permission> permissionsForUser(UserEntity user) {
    List<AccountPermission> accountPermissions = userRepository
        .findByIdAndAccountId(user.getId(), user.getAccountId())
        .map(UserEntity::getAccountPermissions)
        .orElse(Collections.emptyList());

    return accountPermissions.stream().map(AccountPermission::getName).collect(toList());
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
