package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.caz.vcc.dto.PaymentStatus.NOT_PAID;
import static uk.gov.caz.vcc.dto.PaymentStatus.PAID;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.repository.PaymentsRepository;

@ExtendWith(MockitoExtension.class)
class PaymentsServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private AsyncRestService asyncRestService;

  @InjectMocks
  private RemotePaymentsService paymentsService;

  @Mock
  AsyncOp<List<EntrantPaymentDtoV1>> asyncOp;

  @Test
  public void shouldReturnStatusPaid() {
    //given
    when(paymentsRepository.registerVehicleEntryAsyncV1(any())).thenReturn(asyncOp);
    when(asyncOp.hasError()).thenReturn(false);
    when(asyncOp.getResult()).thenReturn(Collections.singletonList(createPaymentStatusResponse()));

    //when
    EntrantPaymentDtoV1 entrantPaymentDto = paymentsService.registerVehicleEntryAndGetPaymentStatus(
        createPaymentStatusRequest());

    //then
    assertThat(entrantPaymentDto.getPaymentStatus()).isEqualTo(PAID);
  }

  @Test
  public void shouldReturnStatusNotPaid() {
    //given
    when(paymentsRepository.registerVehicleEntryAsyncV1(any())).thenReturn(asyncOp);
    when(asyncOp.hasError()).thenReturn(false);
    when(asyncOp.getResult()).thenReturn(Collections.emptyList());

    //when
    EntrantPaymentDtoV1 entrantPaymentDto = paymentsService.registerVehicleEntryAndGetPaymentStatus(
        createPaymentStatusRequest());

    //then
    assertThat(entrantPaymentDto.getPaymentStatus()).isEqualTo(NOT_PAID);
  }

  private EntrantPaymentDtoV1 createPaymentStatusResponse() {
    return EntrantPaymentDtoV1.builder()
        .paymentStatus(PAID)
        .build();
  }

  private EntrantPaymentRequestDto createPaymentStatusRequest() {
    return EntrantPaymentRequestDto.builder()
        .cleanZoneId(UUID.randomUUID())
        .cazEntryTimestamp(LocalDateTime.now())
        .vrn("SW61BYD")
        .build();
  }
}