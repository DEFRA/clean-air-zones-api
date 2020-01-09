package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.caz.vcc.dto.PaymentStatus.PAID;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.PaymentStatusResponseDto;

@ExtendWith(MockitoExtension.class)
class PaymentsRepositoryTest {

  private String paymentsRootUri = "";

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate paymentsRestTemplate;

  private PaymentsRepository paymentsRepository;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilder.rootUri(anyString())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(paymentsRestTemplate);
    paymentsRepository = new PaymentsRepository(restTemplateBuilder, paymentsRootUri);
  }

  @Test
  void shouldReturnResponseWithStatusPaid() {
    //given
    mockPaymentServiceToReturnStatusPaid();

    //when
    Optional<PaymentStatusResponseDto> paymentStatusResponse = paymentsRepository
        .registerVehicleEntryAndGetPaymentStatus(createPaymentStatusRequest());

    //then
    assertThat(paymentStatusResponse).isPresent();
    assertThat(paymentStatusResponse.get().getStatus()).isEqualTo(PAID);
  }

  @Test
  void shouldReturnEmpty() {
    //given
    mockPaymentServiceToThrowHttpServerErrorException();

    //when
    Optional<PaymentStatusResponseDto> paymentStatusResponse = paymentsRepository
        .registerVehicleEntryAndGetPaymentStatus(createPaymentStatusRequest());

    //then
    assertThat(paymentStatusResponse)
        .isNotPresent()
        .isEmpty();
  }


  @Test
  public void shouldThrowExternalServiceCallExceptionIfAnyRuntimeExceptionWasThrown() {
    //given
    mockPaymentServiceToThrowRuntimeException();

    //then
    assertThrows(ExternalServiceCallException.class,
        () -> paymentsRepository
            .registerVehicleEntryAndGetPaymentStatus(createPaymentStatusRequest()));
  }

  private void mockPaymentServiceToThrowRuntimeException() {
    when(paymentsRestTemplate.exchange(any(), eq(PaymentStatusResponseDto.class)))
        .thenThrow(RuntimeException.class);
  }

  private void mockPaymentServiceToThrowHttpServerErrorException() {
    when(paymentsRestTemplate.exchange(any(), eq(PaymentStatusResponseDto.class)))
        .thenThrow(HttpServerErrorException.class);
  }

  private void mockPaymentServiceToReturnStatusPaid() {
    when(paymentsRestTemplate.exchange(any(), eq(PaymentStatusResponseDto.class)))
        .thenReturn(ResponseEntity.ok(PaymentStatusResponseDto.builder().status(PAID).build()));
  }

  private PaymentStatusRequestDto createPaymentStatusRequest() {
    return PaymentStatusRequestDto.builder()
        .cleanZoneId(UUID.randomUUID())
        .cazEntryTimestamp(LocalDateTime.now())
        .vrn("SW61BYD")
        .build();
  }
}