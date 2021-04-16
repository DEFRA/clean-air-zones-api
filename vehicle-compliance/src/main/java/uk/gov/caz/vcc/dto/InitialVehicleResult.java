package uk.gov.caz.vcc.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/**
 * Helper value object that stores vehicle-entrant-related data.
 */
@Value
@Builder(toBuilder = true)
public class InitialVehicleResult {
  VehicleResultDto result;

  // null when shouldFetchPaymentDetails is true
  VehicleEntrantReportingRequest reportingRequest;

  // null when shouldFetchPaymentDetails is false
  UUID cleanAirZoneId;
  // null when shouldFetchPaymentDetails is false
  LocalDateTime cazEntryTimestamp;
  // null when shouldFetchPaymentDetails is false
  Function<PaymentStatus, VehicleComplianceStatus> complianceStatusProvider;
  // null when shouldFetchPaymentDetails is false
  Function<EntrantPaymentMethodAndStatusSupplier, PaymentMethod> paymentMethodProvider;
  //null when shouldFetchPaymentDetails is false
  Function<VehicleResultDto, VehicleEntrantReportingRequest> reportingRequestBuilder;
  // null when shouldFetchPaymentDetails is false, nullable when shouldFetchPaymentDetails is true
  Function<VehicleComplianceStatus, String> tariffCodeBuilder;

  @Getter(AccessLevel.NONE)
  boolean shouldFetchPaymentDetails;

  /**
   * Method to return whether payment details should be fetched.
   * @return boolean shouldFetchPaymentDetails.
   */
  public boolean shouldFetchPaymentDetails() {
    return shouldFetchPaymentDetails;
  }
}