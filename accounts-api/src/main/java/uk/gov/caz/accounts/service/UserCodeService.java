package uk.gov.caz.accounts.service;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.service.exception.AccountUserCodeNotFoundException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;

/**
 * Service responsible for {@link AccountUserCode} related actions. It is used during reset password
 * process.
 */
@Service
@AllArgsConstructor
public class UserCodeService {

  private final AccountUserCodeRepository accountUserCodeRepository;
  private final AccountUserRepository accountUserRepository;
  private final TokenToHashConverter tokenToHashConverter;
  private final IdentityProvider identityProvider;

  /**
   * Method to get {@link User} for the {@link AccountUserCode} found based on the provided token
   * and its type.
   *
   * @param token Token received from the request.
   * @param codeType Type of the token.
   */
  public User findUserByTokenAndCodeType(UUID token, CodeType codeType) {
    AccountUserCode accountUserCode = accountUserCodeRepository.findByCodeAndCodeType(
        calcHashOf(token),
        codeType
    ).orElseThrow(() -> new AccountUserCodeNotFoundException("AccountUserCode not found."));

    User dbUser = accountUserRepository.findById(accountUserCode.getAccountUserId())
        .orElseThrow(() -> new UserNotFoundException("Cannot process request."));

    User identityProviderUser = getIdentityProviderUserConnectedWith(dbUser);

    return User.combinedDbAndIdentityProvider(dbUser, identityProviderUser);
  }

  /**
   * Given our DB User gets User from IdentityProvider.
   */
  private User getIdentityProviderUserConnectedWith(User dbUser) {
    String userEmail = identityProvider.getEmailByIdentityProviderId(
        dbUser.getIdentityProviderUserId());
    return identityProvider.getUser(userEmail);
  }

  /**
   * Method to mark code status as USED.
   *
   * @param token Token received from the request.
   */
  @Transactional
  public void markCodeAsUsed(UUID token) {
    accountUserCodeRepository.setStatusForCode(calcHashOf(token), CodeStatus.USED);
  }

  /**
   * Method to validate if the provided token is present in DB and active.
   *
   * @param token Hashed token which need to be validated.
   * @param codeType Code type of the token.
   * @return boolean value of code validation
   */
  public boolean isActive(UUID token, CodeType codeType) {
    Optional<AccountUserCode> accountUserCodeOptional = accountUserCodeRepository
        .findByCodeAndCodeType(calcHashOf(token), codeType)
        .filter(AccountUserCode::isActive);
    if (accountUserCodeOptional.isPresent()) {
      AccountUserCode accountUserCode = accountUserCodeOptional.get();
      User dbUser = accountUserRepository.findById(accountUserCode.getAccountUserId())
          .orElseThrow(() -> new UserNotFoundException("Token is invalid or expired"));

      return dbUser.getIdentityProviderUserId() != null;
    }

    return false;
  }

  /**
   * Method which converts received token to hashed code.
   *
   * @param token received from User.
   * @return Hashed token
   */
  private String calcHashOf(UUID token) {
    return tokenToHashConverter.convert(token);
  }
}
