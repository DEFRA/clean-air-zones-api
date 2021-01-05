package uk.gov.caz.accounts.service;

import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.emailnotifications.EmailContext;
import uk.gov.caz.accounts.service.emailnotifications.PasswordResetEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.UserInvitationEmailSender;

/**
 * Service responsible for associating Account with reset password token and then performing the
 * actual password reset.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final UserService userService;
  private final AccountUserCodeRepository accountUserCodeRepository;
  private final TokenToHashConverter tokenToHashConverter;
  private final TokensExpiryDatesProvider tokensExpiryDatesProvider;
  private final PasswordResetEmailSender passwordResetEmailSender;
  private final UserInvitationEmailSender userInvitationEmailSender;

  private static final int UNUSED_RESET_TOKENS_LIMIT = 5;

  /**
   * Method creates new record of {@link AccountUserCode} with generated and hashed UUID, generated
   * expiration date based on configuration and associates it with {@link User} based on provided
   * email.
   *
   * @param email of a user requesting password change.
   * @param passwordResetLink An URI for UI that needs to be used by the user to validate the
   *     token and provide new password.
   */
  @Transactional
  public void generateAndSaveResetToken(String email, URI passwordResetLink) {
    Optional<User> optionalUser = userService.getUserByEmail(email);

    if (optionalUser.isPresent() && resetEmailsLimitNotExceeded(optionalUser.get())
        && noEmailChangeIsInProgress(optionalUser.get())) {
      User user = optionalUser.get();
      log.info("Preparing to reset password to user '{}' with email: '{}'", user.getId(),
          mask(email));
      UUID resetToken = generateAndSaveResetTokenFor(user);
      sendPasswordResetEmail(user, passwordResetLink, resetToken);
      log.info("Successfully sent reset password email to user '{}'", user.getId());
    } else {
      // If User is not present do not do any changes and return 204 like everything was ok.
      // This is not to reveal information to potential attackers.
      log.info("User with email: {} not found. Faking successful password reset response.",
          mask(email));
    }
  }

  /**
   * Verifies if the user is performing email change.
   */
  private boolean noEmailChangeIsInProgress(User user) {
    List<AccountUserCode> emailChangeCodes = accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeType(
            user.getId(),
            CodeStatus.ACTIVE,
            CodeType.EMAIL_CHANGE_VERIFICATION
        );

    return emailChangeCodes.stream().noneMatch(AccountUserCode::isActive);
  }

  /**
   * Creates and saves a new {@link AccountUserCode} with a randomly generated token for the invited
   * {@code user}. Finally an email is sent with the 'activation' (password-reset) link.
   */
  @Transactional
  public void generateAndSaveResetTokenForInvitedUser(User user, Account userAccount,
      URI verificationLink) {
    checkPreconditionsForInvitedUser(user, verificationLink);

    UUID resetToken = generateAndSaveResetTokenFor(user);
    sendUserInvitationEmail(user, userAccount, verificationLink, resetToken);
    log.info("Successfully sent invitation email to user '{}'", user.getId());
  }

  private void sendUserInvitationEmail(User user, Account userAccount, URI invitationLink,
      UUID resetToken) {
    String passwordResetUri = new URIBuilder(invitationLink)
        .addParameter("token", resetToken.toString())
        .addParameter("account", userAccount.getId().toString())
        .toString();
    log.info("User invitation URL: {}", passwordResetUri);
    userInvitationEmailSender.send(user.getEmail(), passwordResetUri, EmailContext.of(userAccount));
  }

  private void saveResetToken(UUID resetToken, User user) {
    AccountUserCode accountUserCode = buildAccountUserCode(user.getId(), resetToken);
    accountUserCodeRepository.save(accountUserCode);
  }

  private UUID generateAndSaveResetTokenFor(User user) {
    UUID resetToken = UUID.randomUUID();
    saveResetToken(resetToken, user);
    return resetToken;
  }

  private void checkPreconditionsForInvitedUser(User user, URI verificationLink) {
    Preconditions.checkNotNull(user, "user cannot be null");
    Preconditions.checkNotNull(verificationLink, "verificationLink cannot be null");
    Preconditions.checkArgument(!user.isOwner(), "user cannot be an owner");
    Preconditions.checkNotNull(user.getAdministeredBy(), "User#administeredBy must be non-null");
    Preconditions.checkArgument(!user.isEmailVerified(), "user cannot have a verified email");
    Preconditions.checkArgument(StringUtils.hasText(user.getEmail()), "user must have a non-empty "
        + "email");
  }

  /**
   * Sends email with URL to reset password.
   *
   * @param user recipient of the email ({@link User#getEmail()})
   * @param passwordResetLink base URL for password reset.
   * @param resetToken token specialised for specific user.
   */
  private void sendPasswordResetEmail(User user, URI passwordResetLink, UUID resetToken) {
    String passwordResetUri = new URIBuilder(passwordResetLink)
        .addParameter("token", resetToken.toString())
        .toString();
    log.info("Password Reset URL: {}", passwordResetUri);
    passwordResetEmailSender.send(user.getEmail(), passwordResetUri);
  }

  /**
   * Method creates default {@link AccountUserCode} object for provided accountUserId.
   *
   * @param accountUserId id of a user requesting password change.
   * @param resetToken Raw token that will be used to validate user's request to reset
   *     password.
   * @return {@link AccountUserCode} which was created for provided user.
   */
  private AccountUserCode buildAccountUserCode(UUID accountUserId, UUID resetToken) {
    return AccountUserCode.builder()
        .accountUserId(accountUserId)
        .expiration(generateExpirationDate())
        .code(tokenToHashConverter.convert(resetToken))
        .codeType(CodeType.PASSWORD_RESET)
        .status(CodeStatus.ACTIVE)
        .build();
  }

  /**
   * Method fetches last 5 reset tokens of the provided user. When user has less than 5 tokens OR
   * any of the fetched codes is used, then method returns TRUE.
   *
   * @param user user which tries to reset password
   * @return boolean
   */
  private boolean resetEmailsLimitNotExceeded(User user) {
    List<AccountUserCode> codes = accountUserCodeRepository
        .findByAccountUserIdFromLastHourWithLimit(
            user.getId(),
            CodeType.PASSWORD_RESET.toString(),
            UNUSED_RESET_TOKENS_LIMIT);

    return codes.size() < UNUSED_RESET_TOKENS_LIMIT
        || codes.stream().anyMatch(code -> code.getStatus() == CodeStatus.USED);
  }

  /**
   * Method generates expiration date for the reset token.
   *
   * @return LocalDateTime object which specifies date time for token expiration.
   */
  private LocalDateTime generateExpirationDate() {
    return tokensExpiryDatesProvider.getResetTokenExpiryDateFromNow();
  }
}
