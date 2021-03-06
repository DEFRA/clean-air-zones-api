package uk.gov.caz.psr.service.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.messaging.MessagingClient;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.events.PaymentStatusUpdatedEvent;
import uk.gov.caz.psr.repository.PaymentRepository;
import uk.gov.caz.psr.service.PaymentReceiptEmailCreator;

@ExtendWith(MockitoExtension.class)
public class PaymentReceiptSenderTest {

  private static final String ANY_VALID_EMAIL = "test@test.com";
  private static final int ANY_AMOUNT = 800;
  private static final Long ANY_REFERENCE = 1001L;
  private static final String ANY_VRN = "VRN123";
  private static final String ANY_EXT_ID = "ext-id";
  private static final LocalDate ANY_TRAVEL_DATE = LocalDate.of(2019, 2, 6);
  private static final Payment ANY_PAYMENT_WITHOUT_ENTRANT_PAYMENTS = Payment.builder()
      .id(UUID.randomUUID())
      .externalId(ANY_EXT_ID)
      .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
      .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
      .authorisedTimestamp(LocalDateTime.now())
      .referenceNumber(ANY_REFERENCE)
      .totalPaid(ANY_AMOUNT)
      .entrantPayments(Collections.emptyList())
      .emailAddress(ANY_VALID_EMAIL)
      .build();
  private static final Payment ANY_PAYMENT = ANY_PAYMENT_WITHOUT_ENTRANT_PAYMENTS.toBuilder()
      .entrantPayments(Collections.singletonList(EntrantPayment.builder()
          .updateActor(EntrantPaymentUpdateActor.USER)
          .charge(10)
          .cleanAirZoneId(UUID.randomUUID())
          .vrn(ANY_VRN)
          .internalPaymentStatus(InternalPaymentStatus.PAID)
          .travelDate(ANY_TRAVEL_DATE)
          .tariffCode("some-tariff")
          .build()))
      .build();

  public static final Set<String> EMAILS_TO_SKIP =
      Sets.newHashSet("skip-man@informed.com", "skip-2nd.informed.com");

  @Mock
  MessagingClient messagingClient;

  @Mock
  PaymentReceiptEmailCreator paymentReceiptEmailCreator;

  @Mock
  PaymentRepository paymentRepository;

  @Spy
  Set<String> emailsToSkip = EMAILS_TO_SKIP;

  @InjectMocks
  PaymentReceiptSender paymentReceiptSender;

  @Test
  public void shouldNotThrowIllegalArgumentExceptionAndNotPublishSQSMessageWhenEmailIsNull() {
    // given
    PaymentStatusUpdatedEvent event = eventWithNullEmail();

    // when
    paymentReceiptSender.onPaymentStatusUpdated(event);

    // then
    verify(messagingClient, never()).publishMessage(any());
  }

  @Test
  public void shouldNotThrowIllegalArgumentExceptionAndNotPublishSQSMessageWhenEmailIsEmpty() {
    // given
    PaymentStatusUpdatedEvent event = eventWithEmptyEmail();

    // when
    paymentReceiptSender.onPaymentStatusUpdated(event);

    // then
    verify(messagingClient, never()).publishMessage(any());
  }

  @Test
  void shouldNotPropagateExceptionUponMessagePublicationError() {
    // given
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, ANY_PAYMENT);
    SendEmailRequest sendEmailRequest = anyValidRequest();
    when(paymentReceiptEmailCreator.createSendEmailRequest(ANY_PAYMENT)).thenReturn(sendEmailRequest);
    doThrow(new RuntimeException("something")).when(messagingClient).publishMessage(sendEmailRequest);

    // when
    Throwable throwable = catchThrowable(() -> paymentReceiptSender.onPaymentStatusUpdated(event));

    // then
    assertThat(throwable).isNull();
  }

  @Test
  void shouldPublishMessage() {
    // given
    PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, ANY_PAYMENT);
    SendEmailRequest sendEmailRequest = anyValidRequest();
    when(paymentReceiptEmailCreator.createSendEmailRequest(ANY_PAYMENT)).thenReturn(sendEmailRequest);

    // when
    paymentReceiptSender.onPaymentStatusUpdated(event);

    // then
    verify(messagingClient).publishMessage(sendEmailRequest);
    verify(paymentRepository).markSentConfirmationEmail(ANY_PAYMENT.getId());
  }

  @Test
  void shouldNotPublishMessageIfEmailIsDeclaredAsToBeSkipped() {
    for (String emailToSkip : emailsToSkip) {
      // given
      Payment payment = ANY_PAYMENT.toBuilder().emailAddress(emailToSkip).build();
      PaymentStatusUpdatedEvent event = new PaymentStatusUpdatedEvent(this, payment);

      // when
      paymentReceiptSender.onPaymentStatusUpdated(event);

      // then
      verifyNoInteractions(messagingClient);
      verify(paymentRepository, never()).markSentConfirmationEmail(ANY_PAYMENT.getId());
    }
  }

  private SendEmailRequest anyValidRequest() {
    return SendEmailRequest.builder().templateId("test-template-id").emailAddress(ANY_VALID_EMAIL)
        .personalisation("{\"amount\":" + ANY_AMOUNT + "}").build();
  }

  private PaymentStatusUpdatedEvent eventWithNullEmail() {
    return new PaymentStatusUpdatedEvent(this, buildPaymentWithEmail(null));
  }

  private PaymentStatusUpdatedEvent eventWithEmptyEmail() {
    return new PaymentStatusUpdatedEvent(this, buildPaymentWithEmail(""));
  }

  private Payment buildPaymentWithEmail(String s) {
    return ANY_PAYMENT.toBuilder().emailAddress(s).build();
  }
}
