package uk.gov.caz.accounts.service.emailnotifications;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.accounts.dto.SendEmailRequest;

/**
 * Abstract class which is responsible for email sending. Child classes provides only constructors.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class UserEmailSender {

  private final UserEmailService emailService;
  private final EmailNotificationSender emailNotificationSender;

  /**
   * Sends a message with verification URI for a given recipient.
   *
   * @param email specifies the recipient
   * @param verificationEmailUri link to verify the account
   * @param context Context of the operation, cannot be null.
   */
  public void send(String email, String verificationEmailUri, EmailContext context) {
    Preconditions.checkNotNull(context, "Context cannot be null");

    SendEmailRequest request = emailService.buildSendEmailRequest(email, verificationEmailUri,
        context);
    emailNotificationSender.send(request);
  }

  /**
   * Sends an email with the verification URI to {@code email}.
   */
  public void send(String email, String verificationEmailUri) {
    send(email, verificationEmailUri, EmailContext.empty());
  }
}
