package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

/**
 * Abstract class which provides methods for email construction. Child classes provide only
 * constructors.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class UserEmailService {

  private final String templateId;
  private final ObjectMapper objectMapper;
  private final String urlAttributeName;

  /**
   * Creates a SendEmailRequest object.
   *
   * @param email the recipient of the email
   * @param context Context of the operation
   * @return SendEmailRequest
   * @throws EmailSerializationException if serialization fails
   */
  public SendEmailRequest buildSendEmailRequest(String email, String verificationTokenUrl,
      EmailContext context) {
    return SendEmailRequest.builder()
        .emailAddress(email)
        .templateId(templateId)
        .personalisation(createPersonalisationPayload(verificationTokenUrl, context))
        .build();
  }

  /**
   * A skeleton implementation of a custom payload attributes - to be implemented in subclasses
   * if needed.
   * @param context The context of the operation (contains objects related to this operation)
   */
  protected Map<String, Object> createPersonalisationMap(EmailContext context) {
    return Collections.emptyMap();
  }

  /**
   * Creates a custom payload which is required to send an email.
   *
   * @param verificationTokenUrl URL required to verify email
   * @param context object containing optionally the user and the account related to this email
   * @return serialized param to JSON payload
   * @throws EmailSerializationException if serialization fails
   */
  private String createPersonalisationPayload(String verificationTokenUrl,
      EmailContext context) {
    Map<String, Object> personalisationMap = computePersonalisationMap(verificationTokenUrl,
        context);

    try {
      return objectMapper.writeValueAsString(personalisationMap);
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize message data.");
      throw new EmailSerializationException("Could not serialize a message.");
    }
  }

  /**
   * Creates a personalisation map that is used to carry variables that are used in the email
   * template.
   */
  private Map<String, Object> computePersonalisationMap(String verificationTokenUrl,
      EmailContext context) {
    Map<String, Object> unfilteredPersonalisationMap = new HashMap<>(createPersonalisationMap(
        context));

    // we need to override the value under 'urlAttributeName' key if provided
    // by 'createPersonalisationMap'
    Object previousValue = unfilteredPersonalisationMap.remove(urlAttributeName);
    if (previousValue != null) {
      log.warn("Previous value stored under key '{}' will be overridden", urlAttributeName);
    }
    unfilteredPersonalisationMap.put(urlAttributeName, verificationTokenUrl);

    return unfilteredPersonalisationMap;
  }
}
