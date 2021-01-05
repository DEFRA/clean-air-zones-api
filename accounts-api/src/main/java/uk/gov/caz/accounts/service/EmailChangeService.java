package uk.gov.caz.accounts.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.emailnotifications.EmailChangeEmailSender;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;

/**
 * Manages the process of changing the user's email address.
 */
@Service
@Slf4j
@AllArgsConstructor
public class EmailChangeService {

  private final UserRepository userRepository;
  private final IdentityProvider identityProvider;
  private final TokenToHashConverter tokenToHashConverter;
  private final TokensExpiryDatesProvider tokensExpiryDatesProvider;
  private final AccountUserCodeRepository accountUserCodeRepository;
  private final EmailChangeEmailSender emailChangeEmailSender;

  /**
   * Initiates the process of changing an email for a user identified by {@code accountUserId}.
   */
  @Transactional
  public void initiateEmailChange(UUID accountUserId, String newEmail, URI rootUrl) {
    UserEntity dbUser = checkPreconditions(accountUserId, newEmail);

    deleteUserInExternalProviderIfExistingUserHasPendingUserId(dbUser);

    UUID pendingUserId = UUID.randomUUID();
    dbUser.setPendingUserId(pendingUserId);

    cloneExistingUserWithNewEmailAndExternalIdentifier(newEmail, pendingUserId,
        dbUser.getIdentityProviderUserId());

    discardAllActiveExistingCodes(accountUserId);
    UUID token = generateAndSaveEmailChangeVerificationTokenFor(dbUser);
    sendCompleteEmailChangeEmail(token, newEmail, rootUrl);
  }

  public void discardEmailChange(UserEntity user) {
    deleteUserInExternalProviderIfExistingUserHasPendingUserId(user);
    userRepository.clearPendingUserId(user.getId());
  }

  /**
   * Discards all active user codes for the given user.
   */
  private void discardAllActiveExistingCodes(UUID accountUserId) {
    List<AccountUserCode> activeCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeTypeIn(accountUserId,
            CodeStatus.ACTIVE,
            Arrays.asList(CodeType.PASSWORD_RESET, CodeType.USER_VERIFICATION,
                CodeType.EMAIL_CHANGE_VERIFICATION)
        );
    activeCodes.forEach(code -> code.setStatus(CodeStatus.DISCARDED));
  }

  /**
   * Sends the email to complete the process of changing the user's email.
   */
  private void sendCompleteEmailChangeEmail(UUID token, String newEmail, URI rootUrl) {
    String completeEmailChangeUri = new URIBuilder(rootUrl)
        .addParameter("token", token.toString())
        .toString();
    log.info("Complete email change URL: {}", completeEmailChangeUri);
    emailChangeEmailSender.send(newEmail, completeEmailChangeUri);
  }

  /**
   * Creates a new user based on the existing one.
   */
  private void cloneExistingUserWithNewEmailAndExternalIdentifier(String newEmail,
      UUID pendingUserId, UUID existingUserIdentityProviderUserId) {
    identityProvider.cloneUserAndSetEmailTo(
        existingUserIdentityProviderUserId,
        pendingUserId,
        newEmail
    );
  }

  /**
   * Verifies if it is possible to change user's email address.
   */
  private UserEntity checkPreconditions(UUID accountUserId, String newEmail) {
    UserEntity user = userRepository.findById(accountUserId)
        .filter(userEntity -> !userEntity.isRemoved())
        .orElseThrow(() -> new AccountUserNotFoundException("User not found"));

    if (userExistsAndEmailDoesNotBelongToHisPendingUser(user, newEmail)) {
      throw new NotUniqueEmailException("New email is not unique");
    }

    return user;
  }

  /**
   * Verifies if the new email address already exists in the identity provider and when it exists it
   * verifies if the email belongs to the same user.
   */
  private boolean userExistsAndEmailDoesNotBelongToHisPendingUser(UserEntity user,
      String newEmail) {
    boolean userExists = identityProvider.checkIfUserExists(newEmail);
    if (!userExists) {
      return false;
    }
    Optional<UUID> pendingUserId = user.getPendingUserId();
    if (pendingUserId.isPresent()) {
      String pendingUserEmail = identityProvider
          .getEmailByIdentityProviderId(pendingUserId.get());

      return !pendingUserEmail.equals(newEmail);
    }

    return true;
  }

  /**
   * Deletes the user from Cognito if he has a non null {@code pendingUserId}.
   */
  private void deleteUserInExternalProviderIfExistingUserHasPendingUserId(UserEntity dbUser) {
    dbUser.getPendingUserId().ifPresent(this::deletePendingUserInExternalService);
  }

  /**
   * Deletes the user in the external service.
   */
  private void deletePendingUserInExternalService(UUID existingPendingUserId) {
    String email = identityProvider.getEmailByIdentityProviderId(existingPendingUserId);
    identityProvider.deleteUser(email);
  }

  /**
   * Generates and saves the user code in the database.
   */
  private UUID generateAndSaveEmailChangeVerificationTokenFor(UserEntity user) {
    UUID emailChangeVerificationToken = UUID.randomUUID();
    saveEmailChangeVerificationToken(emailChangeVerificationToken, user);
    return emailChangeVerificationToken;
  }

  /**
   * Save the generated token in the database.
   */
  private void saveEmailChangeVerificationToken(UUID emailChangeVerificationToken,
      UserEntity user) {
    AccountUserCode userCode = buildAccountUserCode(user.getId(), emailChangeVerificationToken);
    accountUserCodeRepository.save(userCode);
  }

  /**
   * Creates an instance of {@link AccountUserCode}.
   */
  private AccountUserCode buildAccountUserCode(UUID accountUserId,
      UUID emailChangeVerificationToken) {
    return AccountUserCode.builder()
        .accountUserId(accountUserId)
        .expiration(tokensExpiryDatesProvider.getEmailChangeTokenExpiryDateFromNow())
        .code(tokenToHashConverter.convert(emailChangeVerificationToken))
        .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .build();
  }
}
