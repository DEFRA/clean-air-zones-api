package uk.gov.caz.accounts.service.emailnotifications;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;

/**
 * Sends reminder to the account owner of 165th day of account inactivity.
 */
@Component
@RequiredArgsConstructor
public class InactiveFor175DaysEmailSender {

  private final InactiveFor175DaysEmailService inactiveFor175DaysEmailService;
  private final EmailNotificationSender emailNotificationSender;

  /**
   * Sends a reminder about account inactivation.
   *
   * @param email specifies the recipient
   * @param context stores personalised data
   */
  public void send(String email, EmailContext context) {
    Preconditions.checkNotNull(context, "Context cannot be null");

    SendEmailRequest request = inactiveFor175DaysEmailService.buildSendEmailRequest(email, context);
    emailNotificationSender.send(request);
  }
}
