package uk.gov.caz.accounts.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;

/**
 * Service responsible for updating statuses of old AccountUserCodes.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CleanupActiveCodesService {

  private final AccountUserCodeRepository accountUserCodeRepository;

  /**
   * Finds all AccountUserCodes connected with User in the database, check if any are espired and
   * updates them in the database.
   */
  @Transactional
  public void updateExpiredPasswordResetCodesForUser(UserEntity user) {
    log.info("Cleaning up Active AccountUserCodes - start");
    List<AccountUserCode> activeUserCodes = getAllActivePasswordResetCodesForUser(user);
    log.info("Found {} Active AccountUserCodes for users", activeUserCodes.size());
    for (AccountUserCode accountUserCode : activeUserCodes) {
      if (accountUserCode.isExpired()) {
        expireAccountUserCode(accountUserCode);
      }
    }
    log.info("Cleaning up Active AccountUserCodes - end");
  }

  private List<AccountUserCode> getAllActivePasswordResetCodesForUser(UserEntity user) {
    return accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(user.getId(),
        CodeStatus.ACTIVE, CodeType.PASSWORD_RESET);
  }

  private void expireAccountUserCode(AccountUserCode accountUserCode) {
    accountUserCodeRepository.setStatusForCode(accountUserCode.getCode(), CodeStatus.EXPIRED);
  }
}
