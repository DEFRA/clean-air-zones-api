package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that handles SendEmailRequest creation for password reset.
 */
@Component
public class PasswordResetEmailService extends UserEmailService {

  private static final String URL_ATTRIBUTE_NAME = "reset_token";

  /**
   * Constructor for the {@link PasswordResetEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param objectMapper maps parameters to JSON.
   */
  public PasswordResetEmailService(@Value("${services.sqs.template-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper, URL_ATTRIBUTE_NAME);
  }
}
