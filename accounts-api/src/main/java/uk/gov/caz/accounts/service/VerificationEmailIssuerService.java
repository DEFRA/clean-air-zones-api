package uk.gov.caz.accounts.service;

import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.emailnotifications.VerificationEmailSender;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationEmailIssuerService {

  private final TokenToHashConverter tokenToHashConverter;
  private final VerificationEmailSender verificationEmailSender;
  private final AccountUserCodeRepository accountUserCodeRepository;

  /**
   * Method generates UUID token and stores it in the database. The token is going to be used for
   * verification of the created account.
   * @param user successfully created {@link User}
   * @param verificationUri URI for account verification
   */
  @Transactional
  public void generateVerificationTokenAndSendVerificationEmail(UserEntity user,
      URI verificationUri, LocalDateTime tokenExpirationDateTime) {
    Preconditions.checkArgument(StringUtils.hasText(user.getEmail()),
        "user's email must not be blank");

    log.info("Preparing to send verification email for: {}", mask(user.getEmail()));

    UUID verificationToken = UUID.randomUUID();
    AccountUserCode accountUserCode = buildAccountUserVerificationCode(user.getId(),
        verificationToken, tokenExpirationDateTime);
    accountUserCodeRepository.save(accountUserCode);
    sendVerificationEmail(user.getEmail(), verificationUri, verificationToken);
  }

  /**
   * Method sends verification email to the recipient.
   *
   * @param email recipient of the email
   * @param verificationUri base path required for user verification
   * @param verificationToken token used to validate the user
   */
  private void sendVerificationEmail(String email, URI verificationUri, UUID verificationToken) {
    String verificationEmailUri = buildVerificationEmailUri(verificationUri, verificationToken);
    log.info("Verification email URL: {}", verificationEmailUri);
    verificationEmailSender.send(email, verificationEmailUri);
  }

  /**
   * Helper method which builds full verification link.
   *
   * @param verifyEmailLink base path required for user verification
   * @param verificationToken token used to validate the user
   * @return String holding link to validate the user
   */
  private String buildVerificationEmailUri(URI verifyEmailLink, UUID verificationToken) {
    return new URIBuilder(verifyEmailLink)
        .addParameter("token", verificationToken.toString())
        .toString();
  }

  /**
   * Method creates {@link AccountUserCode} record with validation token.
   *
   * @param accountUserId id of the user which is going to be verified
   * @param verificationToken token which will be used to verify the user
   * @return {@link AccountUserCode} the persisted record
   */
  private AccountUserCode buildAccountUserVerificationCode(UUID accountUserId,
      UUID verificationToken, LocalDateTime tokenExpirationDateTime) {
    return AccountUserCode.builder()
        .accountUserId(accountUserId)
        .expiration(tokenExpirationDateTime)
        .code(tokenToHashConverter.convert(verificationToken))
        .codeType(CodeType.USER_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .build();
  }
}
