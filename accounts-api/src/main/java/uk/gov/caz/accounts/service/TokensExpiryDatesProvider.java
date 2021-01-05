package uk.gov.caz.accounts.service;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokensExpiryDatesProvider {

  private final int passwordResetMinutesToExpire;
  private final int verificationTokenMinutesToExpire;
  private final int emailChangeTokenMinutesToExpire;

  /**
   * Creates an instance of {@link TokensExpiryDatesProvider}.
   */
  public TokensExpiryDatesProvider(
      @Value("${application.password-reset-token-in-minutes:1440}")
          int passwordResetMinutesToExpire,
      @Value("${application.verification-token-expiry-in-minutes:1440}")
          int verificationTokenMinutesToExpire,
      @Value("${application.email-change-token-expiry-in-minutes:60}")
          int emailChangeTokenMinutesToExpire) {
    this.passwordResetMinutesToExpire = passwordResetMinutesToExpire;
    this.verificationTokenMinutesToExpire = verificationTokenMinutesToExpire;
    this.emailChangeTokenMinutesToExpire = emailChangeTokenMinutesToExpire;
  }

  /**
   * Method returns expiry datetime for password reset token.
   *
   * @return {@link LocalDateTime} specifying datetime of token expiration.
   */
  public LocalDateTime getResetTokenExpiryDateFromNow() {
    return currentTime().plusMinutes(passwordResetMinutesToExpire);
  }

  /**
   * Method returns expiry datetime for verification token.
   *
   * @return {@link LocalDateTime} specifying datetime of token expiration.
   */
  public LocalDateTime getVerificationEmailExpiryDateFromNow() {
    return currentTime().plusMinutes(verificationTokenMinutesToExpire);
  }

  /**
   * Method returns expiry datetime for email change verification token.
   *
   * @return {@link LocalDateTime} specifying datetime of token expiration.
   */
  public LocalDateTime getEmailChangeTokenExpiryDateFromNow() {
    return currentTime().plusMinutes(emailChangeTokenMinutesToExpire);
  }

  /**
   * Helper method that returns current time.
   *
   * @return {@link LocalDateTime} specifying the current time.
   */
  private LocalDateTime currentTime() {
    return LocalDateTime.now();
  }
}
