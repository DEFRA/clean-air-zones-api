package uk.gov.caz.accounts.service;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.InvalidActiveVerificationCodesAmount;

/**
 * Service responsible for creating new verification token and re-sending the verification email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailResendService {

  private final UserService userService;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final AccountUserCodeRepository accountUserCodeRepository;
  private final VerificationEmailIssuerService verificationEmailIssuerService;

  /**
   * Method manages the process of discarding previous user codes, sets expiration time for the new
   * verification token and sends message to {@link VerificationEmailResendService} to send
   * verification email.
   *
   * @param accountId id of the account
   * @param accountUserId if of the user
   * @param verificationUri URI leading to verification endpoint.
   * @return {@link UserEntity} which is going to be verified.
   */
  @Transactional
  public UserEntity resendVerificationEmail(UUID accountId, UUID accountUserId,
      URI verificationUri) {
    checkPreconditions(accountId, accountUserId, verificationUri);

    LocalDateTime expirationTime = discardActiveUserCodesAndGetExpirationTime(accountUserId);
    UserEntity user = getUserToVerify(accountUserId);

    verificationEmailIssuerService.generateVerificationTokenAndSendVerificationEmail(user,
        verificationUri, expirationTime);

    return user;
  }

  /**
   * Method fetches all required data about user which is going to be verified.
   *
   * @param accountUserId id of the user
   * @return {@link UserEntity} which is going to be verified
   */
  private UserEntity getUserToVerify(UUID accountUserId) {
    return userService.getCompleteUserDetailsAsUserEntityForAccountUserId(accountUserId);
  }

  /**
   * Method discards the existing active user codes (there should be max. 1 such code), and returns
   * it's expiration time, as the new verification token needs to have same expiration time.
   *
   * @param accountUserId id of the user for which the resend process is being made.
   * @return {@link LocalDateTime} which states the expiration time.
   */
  private LocalDateTime discardActiveUserCodesAndGetExpirationTime(UUID accountUserId) {
    List<AccountUserCode> codes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeType(
            accountUserId,
            CodeStatus.ACTIVE,
            CodeType.USER_VERIFICATION
        );

    if (codes.size() != 1) {
      log.error("Data inconsistency: user is expected to have {} code while he has {}", 1,
          codes.size());
      throw new InvalidActiveVerificationCodesAmount();
    }

    AccountUserCode code = codes.iterator().next();
    LocalDateTime expirationDatetime = code.getExpiration();
    markCodeAsDiscarded(code);

    return expirationDatetime;
  }

  /**
   * Marks provided {@link AccountUserCode} as DISCARDED.
   *
   * @param code code to be discarded
   */
  private void markCodeAsDiscarded(AccountUserCode code) {
    accountUserCodeRepository.save(code.toBuilder().status(CodeStatus.DISCARDED).build());
  }

  /**
   * Method which checks if provided parameters are valid.
   */
  private void checkPreconditions(UUID accountId, UUID accountUserId, URI verificationUri) {
    Preconditions.checkNotNull(accountId, "accountId cannot be null");
    Preconditions.checkNotNull(accountUserId, "accountUserId cannot be null");
    Preconditions.checkNotNull(verificationUri, "verificationUri cannot be null");
    verifyAccountPresence(accountId);
    verifyAccountUserPresence(accountUserId);
  }

  /**
   * Method verifies if account for the provided accountId exists in the database.
   *
   * @param accountId id of the account
   */
  private void verifyAccountPresence(UUID accountId) {
    Optional<Account> account = accountRepository.findById(accountId);

    if (!account.isPresent()) {
      log.info("Account was not found.");
      throw new AccountNotFoundException("Account was not found.");
    }
  }

  /**
   * Method verifies if user for the provided accountUserId exists in the database.
   *
   * @param accountUserId id of the account
   */
  private void verifyAccountUserPresence(UUID accountUserId) {
    Optional<UserEntity> user = userRepository.findById(accountUserId);

    if (!user.isPresent()) {
      log.info("AccountUser was not found.");
      throw new AccountUserNotFoundException("AccountUser was not found.");
    }
  }
}