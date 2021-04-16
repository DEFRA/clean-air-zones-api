package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.vcc.dto.PaymentStatus.NOT_PAID;
import static uk.gov.caz.vcc.dto.PaymentStatus.PAID;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
class PaymentsServiceTestIT extends MockServerTestIT {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  @Autowired
  private PaymentsService paymentsService;

  @AfterEach
  public void clear() {
    mockServer.reset();
  }

  @Test
  public void shouldReturnPaidStatus() {
    // given
    mockPaymentServiceToReturnStatusPaid();

    // when
    EntrantPaymentDtoV1 entrantPaymentDto = paymentsService
        .registerVehicleEntryAndGetPaymentStatus(preparePaymentStatusDto());

    // then
    assertThat(entrantPaymentDto.getPaymentStatus()).isEqualTo(PAID);
  }

  @Test
  public void shouldReturnNotPaidStatus() {
    // given
    mockPaymentServiceToReturnStatusNotPaid();

    // when
    EntrantPaymentDtoV1 entrantPaymentDto = paymentsService
        .registerVehicleEntryAndGetPaymentStatus(
            preparePaymentStatusDto());

    // then
    assertThat(entrantPaymentDto.getPaymentStatus()).isEqualTo(NOT_PAID);
  }

  private void mockPaymentServiceToReturnStatusPaid() {
    callPaymentService(mockServer, "payment-status-paid-response.json");
  }

  private void mockPaymentServiceToReturnStatusNotPaid() {
    callPaymentService(mockServer, "payment-status-vrn2-not-paid-response.json");
  }

  private void callPaymentService(ClientAndServer mockServer, String responseFile) {
    mockServer.when(
        requestPost("/v1/payments/vehicle-entrants", "payment-status-first-request.json"))
        .respond(paymentV1Response(responseFile));
  }

  public EntrantPaymentRequestDto preparePaymentStatusDto() {
    return EntrantPaymentRequestDto.builder()
        .cleanZoneId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
        .cazEntryTimestamp(parseDate("2018-10-01T155223Z"))
        .vrn("SW61BYD")
        .build();
  }

  private LocalDateTime parseDate(String dateTimeOfEntrance) {
    return LocalDateTime.parse(dateTimeOfEntrance, DATE_TIME_FORMATTER);
  }
}