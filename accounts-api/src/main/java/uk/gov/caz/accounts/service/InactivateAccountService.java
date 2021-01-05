package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.UserRepository;

/**
 * Service responsible for marking account as inactive. Service should be called after 180 days of
 * inactivity of the users associated with the account.
 */
@Service
@Slf4j
@AllArgsConstructor
public class InactivateAccountService {

  private final UserRemovalService userRemovalService;
  private final AccountVehicleRepository accountVehicleRepository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;

  /**
   * Method responsible for account inactivation.
   *
   * @param accountId Identifies account for which inactivation is going to be performed.
   */
  @Transactional
  public void inactivateAccount(UUID accountId) {
    Account account = checkPreconditions(accountId);
    accountVehicleRepository.deleteInBulkByAccountId(accountId);
    removeAccountUsers(accountId);
    account.setInactivationTimestamp(LocalDateTime.now());
  }

  /**
   * Method performs validation of the provided accountId parameter and returns {@link Account} when
   * it's valid.
   *
   * @param accountId Identifies account which users are going to be removed.
   * @return found {@link Account}
   */
  private Account checkPreconditions(UUID accountId) {
    Preconditions.checkNotNull(accountId, "accountId cannot be null");
    Account account = accountRepository.findById(accountId).orElse(null);
    Preconditions
        .checkArgument(Objects.nonNull(account), "accountId must point to an existing account");
    Preconditions.checkArgument(account.isActive(), "account must be active");

    return account;
  }

  /**
   * Removes users associated with the provided accountId.
   *
   * @param accountId Identifies account which users are going to be removed.
   */
  private void removeAccountUsers(UUID accountId) {
    List<UserEntity> usersToDelete = userRepository.findAllActiveUsersByAccountId(accountId);
    usersToDelete.stream()
        .forEach(user -> {
          UserRemovalStatus status = userRemovalService
              .removeAnyUser(user.getAccountId(), user.getId());
          if (status != UserRemovalStatus.SUCCESSFULLY_DELETED) {
            log.warn("Error {} occurred while removing user {}", status, user.getId());
          }
        });
  }
}
