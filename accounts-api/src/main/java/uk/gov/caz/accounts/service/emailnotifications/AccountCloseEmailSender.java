package uk.gov.caz.accounts.service.emailnotifications;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;

/**
 * Sends an email confirmation that the account has been closed.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AccountCloseEmailSender {

  private final AccountCloseEmailService accountCloseEmailService;
  private final EmailNotificationSender emailNotificationSender;

  /**
   * Sends an email confirmation that the account has been closed.
   *
   * @param email specifies the recipient
   * @param context stores personalised data
   */
  public void send(String email, EmailContext context) {
    Preconditions.checkNotNull(context, "Context cannot be null");

    SendEmailRequest request = accountCloseEmailService.buildSendEmailRequest(email, context);
    log.info("Sending email about the closed account.");
    emailNotificationSender.send(request);
  }
}
