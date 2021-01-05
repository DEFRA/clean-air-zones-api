package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;

/**
 * Service responsible processing user permissions. It is used during users update
 */
@Service
@AllArgsConstructor
public class UserPermissionsUpdaterService {

  private final UserPermissionsService userPermissionsService;
  private final AccountRepository accountRepository;

  /**
   * Update users permissions. If {@code Permission.MAKE_PAYMENT} assigned to user then updates
   * {@code multiPayerAccount} value for account.
   *
   * @param accountId User's account identifier.
   * @param userId User's identifier.
   * @param newPermissions A collection of permissions that is to be set for the given user.
   */
  @Transactional
  public void update(UUID accountId, UUID userId, Set<Permission> newPermissions) {
    Preconditions.checkNotNull(newPermissions, "newPermissions cannot be null");
    userPermissionsService.updatePermissions(accountId, userId, newPermissions);

    if (newPermissions.contains(Permission.MAKE_PAYMENTS)) {
      Account account = accountRepository.findById(accountId)
          .orElseThrow(() -> new AccountNotFoundException("Account was not found."));

      if (!account.isMultiPayerAccount()) {
        accountRepository.updateMultiPayerAccount(account.getId(), Boolean.TRUE);
      }
    }
  }
}
