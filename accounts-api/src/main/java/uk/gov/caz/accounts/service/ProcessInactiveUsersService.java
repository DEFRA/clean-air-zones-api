package uk.gov.caz.accounts.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountClosureReason;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.emailnotifications.EmailContext;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor165DaysEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor175DaysEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.InactiveFor180DaysEmailSender;

/**
 * Class responsible for detecting if there are any accounts which require notification associated
 * with the inactivity and sending proper message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class ProcessInactiveUsersService {

  private static final int INACTIVE_DAY_165 = 165;
  private static final int INACTIVE_DAY_175 = 175;
  private static final int INACTIVE_DAY_180 = 180;

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final IdentityProvider identityProvider;
  private final InactivateAccountService inactivateAccountService;
  private final InactiveFor165DaysEmailSender inactiveFor165DaysEmailSender;
  private final InactiveFor175DaysEmailSender inactiveFor175DaysEmailSender;
  private final InactiveFor180DaysEmailSender inactiveFor180DaysEmailSender;

  /**
   * Method fetches all accounts and checks if any of them require email notification.
   */
  @Transactional
  public void execute() {
    List<Account> allAccounts = accountRepository.findAllByInactivationTimestampIsNull();
    allAccounts.stream().forEach(this::checkNotificationForAccount);
  }

  /**
   * Method checks when was the latest sign-in of the user belonging to the provided account and
   * sends message if it's required.
   *
   * @param account account for which the notification requirement is being checked.
   */
  private void checkNotificationForAccount(Account account) {
    Optional<Timestamp> latestUserSignIn = userRepository
        .getLatestUserSignInForAccount(account.getId());

    if (latestUserSignIn.isPresent()) {
      sendNotificationIfQualify(account, latestUserSignIn.get().toLocalDateTime());
    }
  }

  /**
   * Method checks for how long the account was not being used and sends proper message.
   *
   * @param account account for which the notification requirement is being checked.
   * @param latestUserSignIn sign-in datetime
   */
  private void sendNotificationIfQualify(Account account, LocalDateTime latestUserSignIn) {
    if (daysOfInactivity(latestUserSignIn) == INACTIVE_DAY_165) {
      getOwnersEmailsForAccount(account).stream()
          .forEach((email) -> inactiveFor165DaysEmailSender.send(email, EmailContext.of(account)));
    } else if (daysOfInactivity(latestUserSignIn) == INACTIVE_DAY_175) {
      getOwnersEmailsForAccount(account).stream()
          .forEach((email) -> inactiveFor175DaysEmailSender.send(email, EmailContext.of(account)));
    } else if (daysOfInactivity(latestUserSignIn) >= INACTIVE_DAY_180) {
      getOwnersEmailsForAccount(account).stream()
          .forEach((email) -> inactiveFor180DaysEmailSender.send(email, EmailContext.of(account)));
      inactivateAccountService.inactivateAccount(account.getId());
      accountRepository.updateClosureReason(
              account.getId(), AccountClosureReason.ACCOUNT_INACTIVITY);
    }
  }

  /**
   * Method fetches owner users of the provided account.
   *
   * @param account account which owners are going to be fetched.
   * @return list of owner emails.
   */
  private List<String> getOwnersEmailsForAccount(Account account) {
    List<UserEntity> accountOwners = userRepository
        .findOwnersForAccount(account.getId());

    return accountOwners
        .stream()
        .map((owner) -> getExistingUserEmail(owner))
        .filter((email) -> email != null)
        .collect(Collectors.toList());
  }

  /**
   * Method used solely for the situation on the DEV environment when local DB state can differ from
   * the IdentityProvider state. When a user is present in the local DB and it cannot be found in
   * the IdentityProvider, instead of raising an exception we skip the non-existing email.
   */
  private String getExistingUserEmail(UserEntity user) {
    String email = null;

    try {
      email = identityProvider
          .getEmailByIdentityProviderId(user.getIdentityProviderUserId());
    } catch (IdentityProviderUnavailableException exception) {
      log.error("Locally stored user was not found in the IdentityProvider");
    }

    return email;
  }

  /**
   * Helper method which counts how many days passed since the provided date.
   *
   * @param latestUserSignIn sign-in datetime
   * @return quantity of days
   */
  private long daysOfInactivity(LocalDateTime latestUserSignIn) {
    return ChronoUnit.DAYS.between(latestUserSignIn.toLocalDate(), LocalDate.now());
  }
}
