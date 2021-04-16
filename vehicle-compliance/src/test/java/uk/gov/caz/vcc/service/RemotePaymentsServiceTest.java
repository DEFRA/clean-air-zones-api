package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.repository.PaymentsRepository;

@ExtendWith(MockitoExtension.class)
class RemotePaymentsServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private AsyncRestService asyncRestService;
  @InjectMocks
  private RemotePaymentsService paymentsService;

  @Nested
  class WhenPassedEmptyCollection {

    @Test
    public void shouldReturnEmptyList() {
      // given
      List<EntrantPaymentRequestDto> requests = Collections.emptyList();

      // when
      List<EntrantPaymentDtoV2> result = paymentsService
          .registerVehicleEntriesAndGetPaymentStatus(requests);

      // then
      assertThat(result).isEmpty();

      verifyNoInteractions(paymentsRepository);
      verifyNoInteractions(asyncRestService);
    }
  }

  @Test
  public void shouldCallPaymentsServiceOnce() {
    // given
    List<EntrantPaymentRequestDto> requests = Arrays.asList(
        createSingleEntrantRequest("CAS301"),
        createSingleEntrantRequest("CAS302")
    );
    mockSuccessfulPaymentsCall(requests);

    // when
    List<EntrantPaymentDtoV2> result = paymentsService
        .registerVehicleEntriesAndGetPaymentStatus(requests);

    // then
    assertThat(result).hasSameSizeAs(requests);

    verify(paymentsRepository).registerVehicleEntryAsyncV2(anyList());
  }

  @Test
  public void shouldThrowExternalServiceCallExceptionUponError() {
    // given
    List<EntrantPaymentRequestDto> requests = Arrays.asList(
        createSingleEntrantRequest("CAS301"),
        createSingleEntrantRequest("CAS302")
    );
    mockFailedPaymentsCall();

    // when
    Throwable result = catchThrowable(
        () -> paymentsService.registerVehicleEntriesAndGetPaymentStatus(requests));

    // then
    assertThat(result).isInstanceOf(ExternalServiceCallException.class);
  }

  private void mockSuccessfulPaymentsCall(List<EntrantPaymentRequestDto> requests) {
    List<EntrantPaymentDtoV2> result = requests.stream()
        .map(request -> EntrantPaymentDtoV2.builder()
            .vrn(request.getVrn())
            .cleanAirZoneId(request.getCleanZoneId())
            .paymentStatus(PaymentStatus.PAID)
            .paymentMethod(PaymentMethod.CARD)
            .entrantPaymentId(UUID.randomUUID())
            .cazEntryTimestamp(LocalDateTime.now())
            .build()
        )
        .collect(Collectors.toList());

    given(paymentsRepository.registerVehicleEntryAsyncV2(anyList())).willReturn(
        AsyncOp.asCompletedAndSuccessful("id", HttpStatus.OK, result)
    );
  }

  private void mockFailedPaymentsCall() {
    given(paymentsRepository.registerVehicleEntryAsyncV2(anyList())).willThrow(
        ExternalServiceCallException.class);
  }

  private EntrantPaymentRequestDto createSingleEntrantRequest(String vrn) {
    return EntrantPaymentRequestDto.builder()
        .vrn(vrn)
        .cazEntryTimestamp(LocalDateTime.now())
        .cleanZoneId(UUID.randomUUID())
        .build();
  }
}