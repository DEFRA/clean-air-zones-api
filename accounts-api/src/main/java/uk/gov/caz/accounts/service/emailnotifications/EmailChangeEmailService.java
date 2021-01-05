package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailChangeEmailService extends UserEmailService {

  public EmailChangeEmailService(
      @Value("${services.sqs.email-change-template-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper, "link");
  }
}
