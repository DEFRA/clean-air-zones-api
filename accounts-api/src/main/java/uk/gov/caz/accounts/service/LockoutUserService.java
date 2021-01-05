package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.service.exception.UserLockoutException;
import uk.gov.caz.accounts.util.Strings;

@Service
@Slf4j
public class LockoutUserService {

  private final IdentityProvider identityProvider;

  private static final String INCORRECT_PASSWORD_OR_USERNAME = "Incorrect password or username";

  private final int maximumAllowedFailedLoginAttempts;

  private final int lockoutTimeoutInMinutes;

  /**
   * Creates an instance of {@link LockoutUserService}.
   */
  public LockoutUserService(IdentityProvider identityProvider,
      @Value("${lockout.login-attempts}") int maximumAllowedFailedLoginAttempts,
      @Value("${lockout.timeout}") int lockoutTimeoutInMinutes) {
    Preconditions.checkArgument(maximumAllowedFailedLoginAttempts >= 1);
    this.identityProvider = identityProvider;
    this.lockoutTimeoutInMinutes = lockoutTimeoutInMinutes;
    this.maximumAllowedFailedLoginAttempts = maximumAllowedFailedLoginAttempts - 1;
  }

  /**
   * Method which locks the account and set failed-logins and lockout-time attributes.
   *
   * @param email {@link String} containing email.
   */
  public void lockoutUserIfApplicable(String email) {
    // user is not locked by lockoutTime
    boolean userExists = checkIfUserExists(email);
    if (!userExists) {
      return;
    }

    if (isLockoutTimeSet(email)) {
      resetLockoutTimeAndSetFailedLoginsToZero(email);
      log.info("Cleared lockoutTime and failedLoginsAttempts for {}", getMaskedEmail(email));
    }

    // time null, failedLoginsAttempts >= 0

    int actualFailedLoginsAttempts = actualFailedLogins(email);
    if (actualFailedLoginsAttempts < maximumAllowedFailedLoginAttempts) {
      increaseFailedLoginsByOne(email);
      log.info("Increased failed-logins for user {}.", getMaskedEmail(email));
      throw new UserLockoutException(INCORRECT_PASSWORD_OR_USERNAME);
    }

    // actualFailedLoginsAttempts >= maximumAllowedFailedLoginAttempts

    increaseFailedLoginsByOne(email);
    setLockoutTime(email);
    log.info("Set lockout-time for user {}.", getMaskedEmail(email));
    throw new UserLockoutException(INCORRECT_PASSWORD_OR_USERNAME);
  }

  /**
   * Method which checks if user is locked by lockout-time.
   *
   * @param email {@link String} containing email.
   */
  public void checkIfUserIsLockedByLockoutTime(String email) {
    if (checkIfUserExists(email) && isLockedByTime(email)) {
      log.info("Account is locked by timeout for user {}.", getMaskedEmail(email));
      throw new UserLockoutException(INCORRECT_PASSWORD_OR_USERNAME);
    }
  }

  /**
   * Method which unlocks the account.
   *
   * @param email {@link String} containing email.
   */
  public void unlockUser(String email) {
    identityProvider.resetFailedLoginsAndLockoutTime(email);
    log.info("Account {} successfully unlocked.", getMaskedEmail(email));
  }

  private boolean isLockoutTimeSet(String email) {
    return getCurrentLockoutTime(email).isPresent();
  }

  private String getMaskedEmail(String email) {
    return Strings.mask(email);
  }

  private boolean checkIfUserExists(String email) {
    return identityProvider.checkIfUserExists(email);
  }

  private void increaseFailedLoginsByOne(String email) {
    identityProvider.increaseFailedLoginsByOne(email);
  }

  private void setLockoutTime(String email) {
    identityProvider.setLockoutTime(email);
  }

  private void resetLockoutTimeAndSetFailedLoginsToZero(String email) {
    identityProvider.resetFailedLoginsAndLockoutTime(email);
  }

  private boolean isLockedByTime(String email) {
    return getCurrentLockoutTime(email)
        .map(currentLockoutTime -> currentLockoutTime.isAfter(currentTimeMinusLockoutTimeout()))
        .orElse(Boolean.FALSE);
  }

  private Optional<LocalDateTime> getCurrentLockoutTime(String email) {
    return identityProvider.getCurrentLockoutTime(email);
  }

  private int actualFailedLogins(String email) {
    return identityProvider.getCurrentFailedLogins(email);
  }

  private LocalDateTime currentTimeMinusLockoutTimeout() {
    return currentTime().minusMinutes(lockoutTimeoutInMinutes);
  }

  private LocalDateTime currentTime() {
    return LocalDateTime.now();
  }
}