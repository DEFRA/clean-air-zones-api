package uk.gov.caz.vcc.service;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV1;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.InitialVehicleResult;
import uk.gov.caz.vcc.dto.VehicleResultDto;

/**
 * Processes vehicle entrants by fetching their payments-related data in a sequential manner.
 */
@RequiredArgsConstructor
public class VehicleEntrantsSequentialPaymentsDataSupplier extends
    VehicleEntrantsPaymentsDataSupplier {

  private final PaymentsService paymentsService;

  @Override
  public List<InitialVehicleResult> processPaymentRelatedEntrants(
      List<InitialVehicleResult> toBeProcessedByPayments) {
    List<InitialVehicleResult> toReturn = Lists.newArrayListWithCapacity(
        toBeProcessedByPayments.size());

    for (InitialVehicleResult initialVehicleResult : toBeProcessedByPayments) {
      EntrantPaymentDtoV1 paymentResult = paymentsService.registerVehicleEntryAndGetPaymentStatus(
          toPaymentStatusRequest(initialVehicleResult));

      VehicleResultDto finalResult = buildFinalResultFrom(paymentResult,
          initialVehicleResult.getResult(),
          initialVehicleResult.getTariffCodeBuilder(),
          initialVehicleResult.getPaymentMethodProvider(),
          initialVehicleResult.getComplianceStatusProvider(),
          paymentResult.getEntrantPaymentId(),
          paymentResult.getTariffCode()
      );

      toReturn.add(
          initialVehicleResult.toBuilder()
              .result(finalResult)
              .build()
      );
    }
    return toReturn;
  }

  private EntrantPaymentRequestDto toPaymentStatusRequest(InitialVehicleResult vehicleResult) {
    return EntrantPaymentRequestDto.builder()
        .vrn(vehicleResult.getResult().getVrn())
        .cleanZoneId(vehicleResult.getCleanAirZoneId())
        .cazEntryTimestamp(vehicleResult.getCazEntryTimestamp())
        .build();
  }
}
