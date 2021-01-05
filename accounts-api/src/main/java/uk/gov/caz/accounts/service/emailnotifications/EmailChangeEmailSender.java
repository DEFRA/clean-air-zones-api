package uk.gov.caz.accounts.service.emailnotifications;

import org.springframework.stereotype.Component;

/**
 * Sender of user's email change notifications.
 */
@Component
public class EmailChangeEmailSender extends UserEmailSender {

  public EmailChangeEmailSender(EmailChangeEmailService emailService,
      EmailNotificationSender emailNotificationSender) {
    super(emailService, emailNotificationSender);
  }
}
