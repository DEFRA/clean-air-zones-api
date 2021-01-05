package uk.gov.caz.accounts.service.emailnotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

/**
 * Class that handles the SendEmailRequest creation for email that inform users about
 * complete charge calculation.
 */
@Component
public class ChargeCalculationCompleteEmailService {

  private final String templateId;

  private final String accountPayUrl;
  
  private final ObjectMapper objectMapper;
  
  private static final String URL_ATTRIBUTE_NAME = "link";
  
  /**
   * Constructor for the {@link ChargeCalculationCompleteEmailService}.
   *
   * @param templateId ID which specifies which template will be used to send email.
   * @param accountPayUrl The URL for the account pay service.
   * @param objectMapper a jackson object mapper instance.
   */
  public ChargeCalculationCompleteEmailService(
      @Value("${services.sqs.charge-calculation-complete-template-id}") String templateId,
      @Value("${services.account-pay.root-url}") String accountPayUrl,
      ObjectMapper objectMapper) {
    this.templateId = templateId;
    this.accountPayUrl = accountPayUrl;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a SendEmailRequest object.
   *
   * @param email the recipient of the email
   * @return SendEmailRequest
   */
  public SendEmailRequest buildSendEmailRequest(String email) {

    HashMap<String, String> personalisations = new HashMap<>(); 
    personalisations.put(URL_ATTRIBUTE_NAME, accountPayUrl);
    
    try {
      return SendEmailRequest.builder()
          .emailAddress(email)
          .personalisation(objectMapper.writeValueAsString(personalisations))
          .templateId(templateId)
          .build();
    } catch (JsonProcessingException e) {
      throw new EmailSerializationException("Could not serialize a message.");
    }
  }
}
