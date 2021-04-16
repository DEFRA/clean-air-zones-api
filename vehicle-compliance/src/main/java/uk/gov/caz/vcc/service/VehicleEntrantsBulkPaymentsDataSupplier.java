package uk.gov.caz.vcc.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.EntrantPaymentRequestDto;
import uk.gov.caz.vcc.dto.InitialVehicleResult;
import uk.gov.caz.vcc.dto.VehicleResultDto;

/**
 * Processes vehicle entrants by fetching their payments-related data in a bulk manner.
 */
@RequiredArgsConstructor
public class VehicleEntrantsBulkPaymentsDataSupplier extends
    VehicleEntrantsPaymentsDataSupplier {

  private final PaymentsService paymentsService;

  @Override
  public List<InitialVehicleResult> processPaymentRelatedEntrants(
      List<InitialVehicleResult> toBeProcessedByPayments) {
    Map<CazEntrantUniqueAttributes, EntrantPaymentDtoV2> groupedEntrantPayments = paymentsService
        .registerVehicleEntriesAndGetPaymentStatus(toPaymentRequests(toBeProcessedByPayments))
        .stream()
        .collect(Collectors.toMap(CazEntrantUniqueAttributes::from, Function.identity(),
            (a, b) -> a));

    return toBeProcessedByPayments.stream()
        .map(initialVehicleResult -> {
          VehicleResultDto result = initialVehicleResult.getResult();
          EntrantPaymentDtoV2 paymentResult = groupedEntrantPayments.get(
              CazEntrantUniqueAttributes.of(
                  initialVehicleResult.getCleanAirZoneId(),
                  initialVehicleResult.getCazEntryTimestamp(),
                  result.getVrn()
              )
          );
          VehicleResultDto finalResult = buildFinalResultFrom(
              paymentResult,
              initialVehicleResult.getResult(),
              initialVehicleResult.getTariffCodeBuilder(),
              initialVehicleResult.getPaymentMethodProvider(),
              initialVehicleResult.getComplianceStatusProvider(),
              paymentResult.getEntrantPaymentId(),
              paymentResult.getTariffCode()
          );
          return initialVehicleResult.toBuilder()
              .result(finalResult)
              .build();
        }).collect(Collectors.toList());
  }

  private List<EntrantPaymentRequestDto> toPaymentRequests(
      List<InitialVehicleResult> toBeProcessedByPayments) {
    return toBeProcessedByPayments.stream()
        .map(this::toPaymentStatusRequest)
        .collect(Collectors.toList());
  }

  private EntrantPaymentRequestDto toPaymentStatusRequest(InitialVehicleResult vehicleResult) {
    return EntrantPaymentRequestDto.builder()
        .vrn(vehicleResult.getResult().getVrn())
        .cleanZoneId(vehicleResult.getCleanAirZoneId())
        .cazEntryTimestamp(vehicleResult.getCazEntryTimestamp())
        .build();
  }

  @Value(staticConstructor = "of")
  private static class CazEntrantUniqueAttributes {
    @NonNull
    UUID cleanAirZoneId;
    @NonNull
    LocalDateTime cazEntryTimestamp;
    @NonNull
    String vrn;

    public static CazEntrantUniqueAttributes from(EntrantPaymentDtoV2 entrantPayment) {
      return new CazEntrantUniqueAttributes(
          entrantPayment.getCleanAirZoneId(),
          entrantPayment.getCazEntryTimestamp(),
          entrantPayment.getVrn()
      );
    }
  }
}
