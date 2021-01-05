package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.model.Account;

/**
 * Class that handles the SendEmailRequest creation for user invitation.
 */
@Component
public class UserInvitationEmailService extends UserEmailService {

  public UserInvitationEmailService(@Value("${services.sqs.invite-template-id}") String templateId,
      ObjectMapper objectMapper) {
    super(templateId, objectMapper, "link");
  }

  @Override
  protected Map<String, Object> createPersonalisationMap(EmailContext context) {
    Account account = context.getAccount()
        .orElseThrow(() -> new IllegalArgumentException("Account cannot be null"));
    return Collections.singletonMap("organisation", account.getName());
  }
}
