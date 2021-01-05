package uk.gov.caz.accounts.service.emailnotifications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.messaging.MessagingClient;

/**
 * Class which is responsible for email sending.
 */
@RequiredArgsConstructor
@Component
public class EmailNotificationSender {

  private final MessagingClient messagingClient;

  /**
   * Sends a message based on provided SendEmailRequest.
   *
   * @param request SendEmailRequest object.
   */
  public void send(SendEmailRequest request) {
    messagingClient.publishMessage(request);
  }
}
