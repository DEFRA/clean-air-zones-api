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
class InactiveFor165DaysEmailSenderTest {

  private static final String ANY_EMAIL = "test@email.com";

  @Mock
  private InactiveFor165DaysEmailService inactiveFor165DaysEmailService;

  @Mock
  private EmailNotificationSender emailNotificationSender;

  @InjectMocks
  private InactiveFor165DaysEmailSender inactiveFor165DaysEmailSender;

  @Test
  public void shouldThrowNullPointerExceptionWhenContextIsNull() {
    // given
    EmailContext context = null;

    // when
    Throwable throwable = catchThrowable(
        () -> inactiveFor165DaysEmailSender.send(ANY_EMAIL, context));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Context cannot be null");
  }

  @Test
  public void shouldSendEmail() {
    // given
    mockSendEmailRequest();

    // when
    inactiveFor165DaysEmailSender.send(ANY_EMAIL, EmailContext.empty());

    // then
    verify(emailNotificationSender).send(any());
  }

  private void mockSendEmailRequest() {
    SendEmailRequest request = SendEmailRequest.builder()
        .emailAddress(ANY_EMAIL)
        .build();
    when(inactiveFor165DaysEmailService.buildSendEmailRequest(anyString(), any()))
        .thenReturn(request);
  }
}
