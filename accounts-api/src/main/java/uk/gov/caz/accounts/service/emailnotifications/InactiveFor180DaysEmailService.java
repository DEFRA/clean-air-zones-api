package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that handles SendEmailRequest creation for a reminder of 165 days of account inactivity.
 */
@Component
public class InactiveFor180DaysEmailService extends InactiveAccountReminderEmailService {

  /**
   * Constructor for the {@link InactiveFor180DaysEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param objectMapper a jackson object mapper instance.
   */
  public InactiveFor180DaysEmailService(
      @Value("${services.sqs.inactivity-180days-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper);
  }
}
