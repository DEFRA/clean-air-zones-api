package uk.gov.caz.accounts.service.validation;

import static uk.gov.caz.accounts.util.Strings.mask;

import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.exception.AccountAlreadyExistsException;

/**
 * Checks whether the account is not a duplicate.
 * The check is done according to the following logic.
 * The business logic should allow creation of an account even if it exists on our database, if:
 * - there are **no users** connected with the account
 * - there are users but only with their **verification link expired**
 * Error should be thrown when there is an account and there is at least one user who is:
 * - active
 * - unverified but link still active
 */
@Service
@Slf4j
@AllArgsConstructor
public class DuplicateAccountValidator {

  private final AccountRepository accountRepository;
  private final AccountUserCodeRepository userCodeRepository;

  /**
   * Validates if the account is not a duplicate according to the logic explained in the class
   * definition.
   * @param accountName being validated.
   */
  public void validate(String accountName) {
    String normalisedAccountName = normaliseAccountName(accountName);
    List<Account> accounts = accountRepository.findAllByNameIgnoreCase(normalisedAccountName);

    if (accounts.size() > 0) {
      List<AccountUserCode> codes = userCodeRepository.findAllByAccountNameForExistingUsers(
          normalisedAccountName);
      checkForVerifiedUsers(codes);
      log.info("Account with a name '{}' exists, but no verified users found (statusCode = USED)",
          mask(accountName));
      checkForPendingUsers(codes);
      log.info("Account with a name '{}' exists, but no pending users found "
          + "(statusCode = ACTIVE and expirationDate in the future)", mask(accountName));
    }
  }

  private String normaliseAccountName(String accountName) {
    return StringUtils.normalizeSpace(accountName).toLowerCase();
  }

  private void checkForVerifiedUsers(List<AccountUserCode> codes) {
    Predicate<AccountUserCode> usersWithTokenUsed =
        userCode -> userCode.getStatus().equals(CodeStatus.USED);
    long verifiedUsersCount = codes.stream().filter(usersWithTokenUsed).count();
    if (verifiedUsersCount > 0) {
      throw new AccountAlreadyExistsException("The company name already exists. "
          + "There are verified users for this account");
    }
  }

  private void checkForPendingUsers(List<AccountUserCode> codes) {
    Predicate<AccountUserCode> usersWaitingForConfirmation =
        userCode -> userCode.isActive();
    long pendingUsersCount = codes.stream().filter(usersWaitingForConfirmation).count();
    if (pendingUsersCount > 0) {
      throw new AccountAlreadyExistsException("The company name already exists. "
          + "There are users pending.");
    }
  }

}