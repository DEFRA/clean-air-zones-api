package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.dto.EntrantPaymentDtoV2;
import uk.gov.caz.vcc.dto.InitialVehicleResult;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.VehicleComplianceStatus;
import uk.gov.caz.vcc.dto.VehicleResultDto;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantsBulkPaymentsDataSupplierTest {

  public static final UUID CLEAN_AIR_ZONE_ID = UUID.randomUUID();
  public static final LocalDateTime CAZ_ENTRY_TIMESTAMP = LocalDateTime.now();
  @Mock
  private PaymentsService paymentsService;

  @InjectMocks
  private VehicleEntrantsBulkPaymentsDataSupplier vehicleEntrantsBulkPaymentsDataSupplier;

  @Nested
  class WhenPassedEmptyCollection {

    @Test
    public void shouldReturnEmptyList() {
      // given
      mockEmptyCollectionResponseFromPayments();
      List<InitialVehicleResult> requests = Collections.emptyList();

      // when
      List<InitialVehicleResult> result = vehicleEntrantsBulkPaymentsDataSupplier
          .processPaymentRelatedEntrants(requests);

      // then
      assertThat(result).isEmpty();
    }

    private void mockEmptyCollectionResponseFromPayments() {
      mockPaymentResults(Collections.emptyList());
    }

  }

  @Test
  public void shouldReturnListWithUpdatedResults() {
    // given
    String vrn = "vrn1";
    PaymentMethod paymentMethod = PaymentMethod.CARD;
    PaymentStatus paymentStatus = PaymentStatus.PAID;
    mockPaymentResults(
        createSingletonListWithResponseFromPaymentWith(vrn, paymentMethod, paymentStatus));
    VehicleResultDto vehicleResult = createVehicleResultWith(vrn);
    InitialVehicleResult element = InitialVehicleResult.builder()
        .result(vehicleResult)
        .cleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .cazEntryTimestamp(CAZ_ENTRY_TIMESTAMP)
        .tariffCodeBuilder(Enum::toString)
        .complianceStatusProvider(status -> VehicleComplianceStatus.COMPLIANT)
        .paymentMethodProvider(e -> paymentMethod)
        .build();

    List<InitialVehicleResult> requests = Collections.singletonList(element);

    // when
    List<InitialVehicleResult> result = vehicleEntrantsBulkPaymentsDataSupplier
        .processPaymentRelatedEntrants(requests);

    // then
    assertThat(result).hasSize(1);
    assertThat(result).hasOnlyOneElementSatisfying(e -> {
      VehicleResultDto finalResult = e.getResult();
      assertThat(finalResult.getStatus()).isEqualTo(VehicleComplianceStatus.COMPLIANT.getStatus());
      assertThat(finalResult.getTariffCode()).isEqualTo(VehicleComplianceStatus.COMPLIANT.toString());
      assertThat(finalResult.getPaymentMethod()).isEqualTo(paymentMethod.toDtoString());
    });
  }

  private void mockPaymentResults(List<EntrantPaymentDtoV2> result) {
    given(paymentsService.registerVehicleEntriesAndGetPaymentStatus(anyList()))
        .willReturn(result);
  }

  private List<EntrantPaymentDtoV2> createSingletonListWithResponseFromPaymentWith(String vrn,
      PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
    return Collections.singletonList(
        EntrantPaymentDtoV2.builder()
            .vrn(vrn)
            .paymentMethod(paymentMethod)
            .paymentStatus(paymentStatus)
            .cleanAirZoneId(CLEAN_AIR_ZONE_ID)
            .cazEntryTimestamp(CAZ_ENTRY_TIMESTAMP)
            .entrantPaymentId(UUID.randomUUID())
            .build()
    );
  }

  private VehicleResultDto createVehicleResultWith(String vrn) {
    return VehicleResultDto.builder()
        .vrn(vrn)
        .build();
  }

}