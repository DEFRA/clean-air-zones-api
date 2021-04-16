package uk.gov.caz.vcc.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;

/**
 * No-op implementation of the payments service.
 */
public class NoOpPaymentsService implements PaymentsService {

  public static final EntrantPaymentDtoV1 NO_OP_RESPONSE_V1 = EntrantPaymentDtoV1.builder()
      .paymentStatus(PaymentStatus.NOT_PAID)
      .paymentMethod(PaymentMethod.NULL)
      .build();

  @Override
  public EntrantPaymentDtoV1 registerVehicleEntryAndGetPaymentStatus(
      EntrantPaymentRequestDto request) {
    return NO_OP_RESPONSE_V1;
  }

  @Override
  public List<EntrantPaymentDtoV2> registerVehicleEntriesAndGetPaymentStatus(
      List<EntrantPaymentRequestDto> toBeProcessedByPayments) {
    return toBeProcessedByPayments.stream()
        .map(request -> EntrantPaymentDtoV2.builder()
            .vrn(request.getVrn())
            .cleanAirZoneId(request.getCleanZoneId())
            .cazEntryTimestamp(request.getCazEntryTimestamp())
            .paymentMethod(NO_OP_RESPONSE_V1.getPaymentMethod())
            .paymentStatus(NO_OP_RESPONSE_V1.getPaymentStatus())
            .build()
        )
        .collect(toList());
  }
}