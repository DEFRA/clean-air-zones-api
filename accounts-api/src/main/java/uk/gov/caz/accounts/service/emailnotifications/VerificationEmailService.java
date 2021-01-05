package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that handles the SendEmailRequest creation for email verification.
 */
@Component
public class VerificationEmailService extends UserEmailService {

  private static final String URL_ATTRIBUTE_NAME = "link";

  /**
   * Constructor for the {@link VerificationEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param objectMapper maps parameters to JSON.
   */
  public VerificationEmailService(
      @Value("${services.sqs.verification-template-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper, URL_ATTRIBUTE_NAME);
  }
}
