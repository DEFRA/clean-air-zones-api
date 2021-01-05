package uk.gov.caz.accounts.service.emailnotifications;

import org.springframework.stereotype.Component;

@Component
public class VerificationEmailSender extends UserEmailSender {

  /**
   * Constructor for {@link VerificationEmailSender}.
   *
   * @param emailNotificationSender provides the service to which an email will be transferred
   * @param verificationEmailService service responsible for verification email construction
   */
  public VerificationEmailSender(EmailNotificationSender emailNotificationSender,
      VerificationEmailService verificationEmailService) {
    super(verificationEmailService, emailNotificationSender);
  }
}
