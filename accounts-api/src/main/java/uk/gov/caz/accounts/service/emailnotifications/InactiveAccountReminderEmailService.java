package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

@AllArgsConstructor
@RequiredArgsConstructor
public abstract class InactiveAccountReminderEmailService {

  private final String templateId;
  private final ObjectMapper objectMapper;

  // Parameter only for 165 and 175 day reminders.
  private String fleetsUrl;

  private static final String URL_ATTRIBUTE_NAME = "link";
  private static final String ORGANISATION_ATTRIBUTE_NAME = "organisation";

  /**
   * Creates a SendEmailRequest object.
   *
   * @param email the recipient of the email
   * @param context stores additional data required to send email
   * @return SendEmailRequest
   */
  public SendEmailRequest buildSendEmailRequest(String email, EmailContext context) {
    return SendEmailRequest.builder()
        .emailAddress(email)
        .personalisation(createPersonalisationPayload(context))
        .templateId(templateId)
        .build();
  }

  /**
   * Creates a custom payload which is required to send an email.
   *
   * @param context stores personalised data
   * @return serialized data
   */
  private String createPersonalisationPayload(EmailContext context) {
    HashMap<String, String> personalisation = new HashMap<>();
    if (StringUtils.isNotBlank(fleetsUrl)) {
      personalisation.put(URL_ATTRIBUTE_NAME, fleetsUrl);
    }
    personalisation.put(ORGANISATION_ATTRIBUTE_NAME, extractOrganisationAttribute(context));

    try {
      return objectMapper.writeValueAsString(personalisation);
    } catch (JsonProcessingException e) {
      throw new EmailSerializationException("Could not serialize a message.");
    }
  }

  /**
   * Extracts specific personalised data.
   *
   * @param context stores personalised data
   * @return extracted attributes
   */
  private String extractOrganisationAttribute(EmailContext context) {
    Account account = context.getAccount()
        .orElseThrow(() -> new IllegalArgumentException("Account cannot be null"));
    return account.getName();
  }
}
