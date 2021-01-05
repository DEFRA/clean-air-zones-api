package uk.gov.caz.accounts.service.emailnotifications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.SendEmailRequest;

@ExtendWith(MockitoExtension.class)
class ChargeCalculationCompleteEmailSenderTest {

  private static final String ANY_EMAIL = "test@email.com";

  @Mock
  private ChargeCalculationCompleteEmailService chargeCalculationCompleteEmailService;

  @Mock
  private EmailNotificationSender emailNotificationSender;

  @InjectMocks
  private ChargeCalculationCompleteEmailSender chargeCalculationCompleteEmailSender;

  @Test
  public void shouldSendEmail() {
    //given
    mockSendEmailRequest();

    //when
    chargeCalculationCompleteEmailSender.send(ANY_EMAIL);

    //then
    verify(emailNotificationSender, times(1)).send(any());
  }

  private void mockSendEmailRequest() {
    SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
        .emailAddress(ANY_EMAIL)
        .build();
    when(chargeCalculationCompleteEmailService.buildSendEmailRequest(ANY_EMAIL))
        .thenReturn(sendEmailRequest);
  }
}