package uk.gov.caz.accounts.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.SendEmailRequest;
import uk.gov.caz.accounts.service.emailnotifications.EmailNotificationSender;
import uk.gov.caz.accounts.service.emailnotifications.PasswordResetEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.PasswordResetEmailService;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailSenderTest {

  private static final String ANY_EMAIL = "any@email.com";
  private static final String ANY_RESET_URI = "http://example.com";

  @Mock
  private EmailNotificationSender emailNotificationSender;

  @Mock
  private PasswordResetEmailService passwordResetEmailService;

  @InjectMocks
  private PasswordResetEmailSender passwordResetEmailSender;

  @Test
  public void shouldSendMessageWhenNoExceptionOccurred() {
    mockValidRequestCreation();
    mockValidMessagePublishing();

    passwordResetEmailSender.send(ANY_EMAIL, ANY_RESET_URI);

    verify(passwordResetEmailService).buildSendEmailRequest(any(), any(), any());
    verify(emailNotificationSender).send(any());
  }

  private void mockValidRequestCreation() {
    SendEmailRequest request = mockSendEmailRequest();

    when(passwordResetEmailService.buildSendEmailRequest(eq(ANY_EMAIL), eq(ANY_RESET_URI), any()))
        .thenReturn(request);
  }

  private void mockValidMessagePublishing() {
    doNothing().when(emailNotificationSender).send(any());
  }

  private SendEmailRequest mockSendEmailRequest() {
    return SendEmailRequest.builder()
        .emailAddress(ANY_EMAIL)
        .templateId(UUID.randomUUID().toString())
        .personalisation("something")
        .build();
  }
}
