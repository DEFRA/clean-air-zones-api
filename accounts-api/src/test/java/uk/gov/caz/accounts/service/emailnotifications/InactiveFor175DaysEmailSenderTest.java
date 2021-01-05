package uk.gov.caz.accounts.service.emailnotifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
class InactiveFor175DaysEmailSenderTest {

  private static final String ANY_EMAIL = "test@email.com";

  @Mock
  private InactiveFor175DaysEmailService inactiveFor175DaysEmailService;

  @Mock
  private EmailNotificationSender emailNotificationSender;

  @InjectMocks
  private InactiveFor175DaysEmailSender inactiveFor175DaysEmailSender;

  @Test
  public void shouldThrowNullPointerExceptionWhenContextIsNull() {
    // given
    EmailContext context = null;

    // when
    Throwable throwable = catchThrowable(
        () -> inactiveFor175DaysEmailSender.send(ANY_EMAIL, context));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Context cannot be null");
  }

  @Test
  public void shouldSendEmail() {
    // given
    mockSendEmailRequest();

    // when
    inactiveFor175DaysEmailSender.send(ANY_EMAIL, EmailContext.empty());

    // then
    verify(emailNotificationSender).send(any());
  }

  private void mockSendEmailRequest() {
    SendEmailRequest request = SendEmailRequest.builder()
        .emailAddress(ANY_EMAIL)
        .build();
    when(inactiveFor175DaysEmailService.buildSendEmailRequest(anyString(), any()))
        .thenReturn(request);
  }
}
