package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.base.Preconditions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.AccountUserPermissionRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;

/**
 * Class responsible for resolving conflicts when user with the same email was already registered
 * but his verification token expired.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DuplicatedAccountUserService {

  private final UserService userService;
  private final AccountUserCodeRepository accountUserCodeRepository;
  private final AccountUserPermissionRepository accountUserPermissionRepository;
  private final UserRepository userRepository;
  private final IdentityProvider identityProvider;

  /**
   * Method fetches existing {@link UserEntity} from Cognito and removes associated verification
   * tokens and the fetched user when it's required.
   *
   * @param email of the already registered user.
   * @throws NotUniqueEmailException when user can still be verified.
   */
  @Transactional
  public void resolveAccountUserDuplication(String email) {
    Preconditions.checkNotNull(email, "email cannot be null");

    UserEntity existingUser = userService.getUserEntityByEmail(email)
        .orElseThrow(() -> new AccountUserNotFoundException("User does not exist"));

    if (existingUser.isEmailVerified()) {
      throw new NotUniqueEmailException("User with given email already exists.");
    }

    if (hasActiveCodes(existingUser)) {
      log.info("Users verification token is not expired therefore email is considered as "
          + "not unique");
      throw new NotUniqueEmailException("User with given email already exists.");
    }

    accountUserCodeRepository.deleteByAccountUserId(existingUser.getId());
    accountUserPermissionRepository.deleteByAccountUserId(existingUser.getId());
    userRepository.delete(existingUser);
    identityProvider.clearPreviousPasswordsForUser(email);
  }

  /**
   * Method counts how many used confirmation tokens are associated with the provided
   * {@link UserEntity}.
   *
   * @param user user which tokens will be fetches.
   * @return true if there are no active codes
   */
  private boolean hasActiveCodes(UserEntity user) {
    List<AccountUserCode> activeCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeTypeIn(user.getId(), CodeStatus.ACTIVE,
            newArrayList(CodeType.USER_VERIFICATION, CodeType.PASSWORD_RESET));

    return activeCodes.stream().anyMatch(AccountUserCode::isActive);
  }
}
