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
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.service.exception.EmailSerializationException;

class InactiveFor165DaysEmailServiceTest {

  private static final String ANY_TEMPLATE_ID = "test-template-id";
  private static final String ANY_URL = "https://example.com";
  private static final String ANY_EMAIL = "sample@email.com";
  private final ObjectMapper objectMapper = mock(ObjectMapper.class);

  private InactiveFor165DaysEmailService inactiveFor165DaysEmailService;

  @BeforeEach
  public void initialize() {
    inactiveFor165DaysEmailService = new InactiveFor165DaysEmailService(ANY_TEMPLATE_ID, ANY_URL,
        objectMapper);
  }

  @Nested
  class BuildSendEmailRequest {

    @Test
    public void shouldReturnSendEmailRequestWhenSuccessfullyCreated()
        throws JsonProcessingException {
      // given
      mockValidEmailRequestCreation();

      // when
      SendEmailRequest request = inactiveFor165DaysEmailService
          .buildSendEmailRequest(ANY_EMAIL, getEmailContextForAccount());

      // then
      assertThat(request.getEmailAddress()).isEqualTo(ANY_EMAIL);
      assertThat(request.getTemplateId()).isEqualTo(ANY_TEMPLATE_ID);
    }

    @Test
    public void shouldThrowEmailSerializationExceptionWhenErrorOccurs()
        throws JsonProcessingException {
      // given
      mockInvalidEmailRequestCreation();

      // when
      Throwable throwable = catchThrowable(() -> inactiveFor165DaysEmailService
          .buildSendEmailRequest(ANY_EMAIL, getEmailContextForAccount()));

      // then
      assertThat(throwable).isInstanceOf(EmailSerializationException.class)
          .hasMessage("Could not serialize a message.");
    }

    @Test
    public void shouldThrowExceptionWhenAccountIsNotPresentInContext()
        throws JsonProcessingException {
      // given
      mockValidEmailRequestCreation();

      // when
      Throwable throwable = catchThrowable(
          () -> inactiveFor165DaysEmailService
              .buildSendEmailRequest(ANY_EMAIL, EmailContext.empty()));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Account cannot be null");
    }

    private void mockValidEmailRequestCreation() throws JsonProcessingException {
      when(objectMapper.writeValueAsString(any())).thenReturn(ANY_URL);
    }

    private void mockInvalidEmailRequestCreation() throws JsonProcessingException {
      when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
    }

    private EmailContext getEmailContextForAccount() {
      Account account = Account.builder().name("Sample Account").build();
      return EmailContext.of(account);
    }
  }
}
