package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that handles SendEmailRequest creation for a reminder of 165 days of account inactivity.
 */
@Component
public class InactiveFor165DaysEmailService extends InactiveAccountReminderEmailService {

  /**
   * Constructor for the {@link InactiveFor165DaysEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param fleetsUrl The URL for the fleets UI service.
   * @param objectMapper a jackson object mapper instance.
   */
  public InactiveFor165DaysEmailService(
      @Value("${services.sqs.inactivity-165days-id}") String templateId,
      @Value("${services.fleets-ui.root-url}") String fleetsUrl,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper, fleetsUrl);
  }
}
