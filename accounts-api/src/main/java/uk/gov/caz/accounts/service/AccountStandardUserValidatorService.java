package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.controller.exception.EmailNotUniqueException;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;

/**
 * Validates if new Standard User will be valid during creation process.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountStandardUserValidatorService {

  private final UserService userService;
  private final AccountUserCodeRepository accountUserCodeRepository;

  /**
   * Validates a standard user in the first step of the user invitation.
   */
  public void validateUserEmail(String email) {
    Preconditions.checkNotNull(email, "email cannot be null");

    Optional<UserEntity> user = userService.getUserEntityByEmail(email);
    if (user.isPresent()) {
      UserEntity existingUser = user.get();
      if (existingUser.isEmailVerified()) {
        throw new EmailNotUniqueException("Provided email is not unique");
      }

      if (hasActiveCodes(existingUser)) {
        log.info("Users verification token is not expired therefore email is considered as "
            + "not unique");
        throw new EmailNotUniqueException("Provided email is not unique");
      }
    }
  }

  /**
   * Method counts how many used confirmation tokens are associated with the provided {@link
   * UserEntity}.
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
