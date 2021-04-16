package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that handles AccountCloseEmailService creation for an account close.
 */
@Component
public class AccountCloseEmailService extends InactiveAccountReminderEmailService {

  /**
   * Constructor for the {@link AccountCloseEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param objectMapper a jackson object mapper instance.
   */
  public AccountCloseEmailService(
      @Value("${services.sqs.account-closure-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper);
  }
}
