package uk.gov.caz.accounts.service.emailnotifications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

/**
 * Class responsible for sending email when charge calculation is successfully completed.
 */
@Component
@RequiredArgsConstructor
public class ChargeCalculationCompleteEmailSender {

  private final ChargeCalculationCompleteEmailService chargeCalculationCompleteEmailService;
  private final EmailNotificationSender emailNotificationSender;

  /**
   * Sends a message with information about finished charge calculation.
   * @param email specifies the recipient
   */
  public void send(String email) throws EmailSerializationException {
    SendEmailRequest request = chargeCalculationCompleteEmailService.buildSendEmailRequest(email);
    emailNotificationSender.send(request);
  }
}