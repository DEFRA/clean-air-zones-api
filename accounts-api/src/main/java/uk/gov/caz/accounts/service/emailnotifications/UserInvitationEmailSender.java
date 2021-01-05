package uk.gov.caz.accounts.service.emailnotifications;

import org.springframework.stereotype.Component;

/**
 * Sends invitation mails to users.
 */
@Component
public class UserInvitationEmailSender extends UserEmailSender {

  public UserInvitationEmailSender(EmailNotificationSender emailNotificationSender,
      UserInvitationEmailService userInvitationEmailService) {
    super(userInvitationEmailService, emailNotificationSender);
  }
}
