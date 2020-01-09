package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.caz.vcc.dto.PaymentStatus.NOT_PAID;
import static uk.gov.caz.vcc.dto.PaymentStatus.PAID;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.PaymentStatusResponseDto;
import uk.gov.caz.vcc.repository.PaymentsRepository;

@ExtendWith(MockitoExtension.class)
class PaymentsServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @InjectMocks
  private PaymentsService paymentsService;

  @Test
  public void shouldReturnStatusPaid() {
    //given
    when(paymentsRepository.registerVehicleEntryAndGetPaymentStatus(any())).thenReturn(createPaymentStatusResponse());

    //when
    PaymentStatus paymentStatus = paymentsService.registerVehicleEntryAndGetPaymentStatus(createPaymentStatusRequest());

    //then
    assertThat(paymentStatus).isEqualTo(PAID);
  }

  @Test
  public void shouldReturnStatusNotPaid() {
    //given
    when(paymentsRepository.registerVehicleEntryAndGetPaymentStatus(any())).thenReturn(Optional.empty());

    //when
    PaymentStatus paymentStatus = paymentsService.registerVehicleEntryAndGetPaymentStatus(createPaymentStatusRequest());

    //then
    assertThat(paymentStatus).isEqualTo(NOT_PAID);
  }

  private Optional<PaymentStatusResponseDto> createPaymentStatusResponse() {
    return Optional.of(PaymentStatusResponseDto.builder()
        .status(PAID)
        .build());
  }

  private PaymentStatusRequestDto createPaymentStatusRequest() {
    return PaymentStatusRequestDto.builder()
        .cleanZoneId(UUID.randomUUID())
        .cazEntryTimestamp(LocalDateTime.now())
        .vrn("SW61BYD")
        .build();
  }
}