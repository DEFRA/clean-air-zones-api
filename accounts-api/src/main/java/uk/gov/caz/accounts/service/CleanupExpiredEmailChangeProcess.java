package uk.gov.caz.accounts.service;

import java.util.List;
import java.util.stream.Collectors;
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
 * Service responsible for cleaning up Expired Email charge process.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CleanupExpiredEmailChangeProcess {

  private final AccountUserCodeRepository accountUserCodeRepository;
  private final EmailChangeService emailChangeService;

  /**
   * Finds all AccountUserCodes connected with User in the database, check if any are expired clean
   * up the expired process.
   */
  @Transactional
  public void cleanupExpiredEmailChangeForUser(UserEntity user) {
    log.info("Cleaning up Expired Email Charge process - start");
    List<AccountUserCode> activeUserCodes = getExpiredEmailChangeCodesForUser(user);
    log.info("Found {} Active but Expired AccountUserCodes for users", activeUserCodes.size());

    for (AccountUserCode accountUserCode : activeUserCodes) {
      expireAccountUserCode(accountUserCode);
    }
    emailChangeService.discardEmailChange(user);
    log.info("Cleaning up Expired Email Charge process - end");
  }


  private List<AccountUserCode> getExpiredEmailChangeCodesForUser(UserEntity user) {
    return accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(user.getId(),
        CodeStatus.ACTIVE, CodeType.EMAIL_CHANGE_VERIFICATION).stream()
        .filter(accountUserCode -> accountUserCode.isExpired())
        .collect(Collectors.toList());
  }

  private void expireAccountUserCode(AccountUserCode accountUserCode) {
    accountUserCodeRepository.setStatusForCode(accountUserCode.getCode(), CodeStatus.EXPIRED);
  }
}
