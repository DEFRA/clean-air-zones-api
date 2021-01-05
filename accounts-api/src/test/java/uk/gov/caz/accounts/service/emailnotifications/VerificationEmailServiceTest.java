package uk.gov.caz.accounts.service.emailnotifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

class VerificationEmailServiceTest {

  private static final String ANY_TEMPLATE_ID = "test-template-id";
  private static final String ANY_EMAIL = "sample@emails.com";
  private static final String ANY_URL = "http://example.com";

  private final ObjectMapper objectMapper = mock(ObjectMapper.class);

  private VerificationEmailService verificationEmailService;

  @BeforeEach
  public void initialize() {
    verificationEmailService = new VerificationEmailService(ANY_TEMPLATE_ID, objectMapper);
  }

  @Nested
  public class BuildSendEmailRequest {

    @Test
    public void shouldReturnSendEmailRequestWhenSuccessfullyCreated()
        throws JsonProcessingException {
      mockValidEmailRequestCreation();

      SendEmailRequest request =
          verificationEmailService.buildSendEmailRequest(ANY_EMAIL, ANY_URL, EmailContext.empty());

      assertThat(request.getPersonalisation()).isEqualTo(ANY_URL);
      assertThat(request.getEmailAddress()).isEqualTo(ANY_EMAIL);
      assertThat(request.getTemplateId()).isEqualTo(ANY_TEMPLATE_ID);
    }

    @Test
    public void shouldThrowEmailSerializationExceptionWhenErrorOccurs()
        throws JsonProcessingException {
      mockInvalidEmailRequestCreation();

      Throwable throwable = catchThrowable(() ->
          verificationEmailService.buildSendEmailRequest(ANY_EMAIL, ANY_URL, EmailContext.empty()));

      assertThat(throwable)
          .isInstanceOf(EmailSerializationException.class)
          .hasMessage("Could not serialize a message.");
    }
  }

  private void mockInvalidEmailRequestCreation() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
  }

  private void mockValidEmailRequestCreation() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any())).thenReturn(ANY_URL);
  }
}