package uk.gov.caz.vcc.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import uk.gov.caz.vcc.dto.EntrantPaymentMethodAndStatusSupplier;
import uk.gov.caz.vcc.dto.InitialVehicleResult;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.VehicleComplianceStatus;
import uk.gov.caz.vcc.dto.VehicleResultDto;

/**
 * Processes vehicle entrants by fetching their payments-related data.
 */
public abstract class VehicleEntrantsPaymentsDataSupplier {

  /**
   * Processes {@code toBeProcessedByPayments} by getting data from PSR and creating a new list of
   * {@link InitialVehicleResult} which will contain a NEW object in {@code
   * uk.gov.caz.vcc.dto.InitialVehicleResult#result} with all payment-related attributes set.
   */
  public abstract List<InitialVehicleResult> processPaymentRelatedEntrants(
      List<InitialVehicleResult> toBeProcessedByPayments);

  /**
   * Creates a new instance of {@link VehicleResultDto} based on the provided arguments.
   */
  protected final VehicleResultDto buildFinalResultFrom(
      EntrantPaymentMethodAndStatusSupplier paymentMethodAndStatusSupplier,
      VehicleResultDto result,
      Function<VehicleComplianceStatus, String> tariffCodeBuilder,
      Function<EntrantPaymentMethodAndStatusSupplier, PaymentMethod> paymentMethodProvider,
      Function<PaymentStatus, VehicleComplianceStatus> complianceStatusProvider,
      UUID entrantPaymentId, String paymentTariffCode) {

    String tariffCode;
    VehicleComplianceStatus complianceStatus = complianceStatusProvider
        .apply(paymentMethodAndStatusSupplier.getPaymentStatus());

    if (complianceStatus.equals(VehicleComplianceStatus.UNRECOGNISED_PAID)) {
      tariffCode = paymentTariffCode;
    } else {
      tariffCode = tariffCodeBuilder == null
          ? result.getTariffCode()
          : tariffCodeBuilder.apply(complianceStatus);
    }

    return result.toBuilder()
        .tariffCode(tariffCode)
        .entrantPaymentId(entrantPaymentId)
        .status(complianceStatus.getStatus())
        .paymentMethod(paymentMethodProvider.apply(paymentMethodAndStatusSupplier).toDtoString())
        .build();
  }
}
