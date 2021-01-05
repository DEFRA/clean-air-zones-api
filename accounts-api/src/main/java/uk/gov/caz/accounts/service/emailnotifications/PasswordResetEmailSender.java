package uk.gov.caz.accounts.service.emailnotifications;

import org.springframework.stereotype.Component;

@Component
public class PasswordResetEmailSender extends UserEmailSender {

  /**
   * Constructor for {@link VerificationEmailSender}.
   *
   * @param emailNotificationSender provides the service to which an email will be transferred
   * @param passwordResetEmailService service responsible for password reset email construction
   */
  public PasswordResetEmailSender(EmailNotificationSender emailNotificationSender,
      PasswordResetEmailService passwordResetEmailService) {
    super(passwordResetEmailService, emailNotificationSender);
  }
}
