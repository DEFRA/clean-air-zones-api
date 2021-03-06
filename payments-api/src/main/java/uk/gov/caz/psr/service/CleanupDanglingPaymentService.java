package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.dto.external.GetPaymentResult;
import uk.gov.caz.psr.model.ExternalPaymentDetails;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.ExternalCardPaymentsRepository;
import uk.gov.caz.psr.util.GetPaymentResultConverter;

/**
 * A service responsible for 'cleaning up' a single dangling payment.
 */
@Service
@Slf4j
@AllArgsConstructor
public class CleanupDanglingPaymentService {

  private final ExternalCardPaymentsRepository externalCardPaymentsRepository;
  private final GetPaymentResultConverter getPaymentResultConverter;
  private final PaymentStatusUpdater paymentStatusUpdater;
  private final EntrantPaymentRepository entrantPaymentRepository;

  /**
   * Processes the passed {@code danglingPayment}: gets the external status and updates it in the
   * database.
   *
   * @return Processed {@code danglingPayment} with the new, updated external status (if different
   *     than the current one) and loaded associated {@link uk.gov.caz.psr.model.EntrantPayment}s.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Payment processDanglingPayment(Payment danglingPayment) {
    checkPreconditions(danglingPayment);

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      danglingPayment = loadEntrantPayments(danglingPayment);
      UUID cleanAirZoneId = getCleanAirZoneId(danglingPayment);
      String externalId = danglingPayment.getExternalId();

      GetPaymentResult paymentInfo = externalCardPaymentsRepository
          .findByIdAndCazId(externalId, cleanAirZoneId)
          .orElseThrow(() -> new IllegalStateException("External payment not found with "
              + "id '" + externalId + "'"));
      ExternalPaymentDetails externalPaymentDetails =
          getPaymentResultConverter.toExternalPaymentDetails(paymentInfo);
      ExternalPaymentStatus status = externalPaymentDetails.getExternalPaymentStatus();
      if (status == danglingPayment.getExternalPaymentStatus()) {
        log.info("The status of a dangling payment has not changed and is equal to '{}', "
            + "aborting the update", danglingPayment.getExternalPaymentStatus());
        return danglingPayment;
      }

      log.info("Updating status of payment '{}' from '{}' to '{}'", danglingPayment.getId(),
          danglingPayment.getExternalPaymentStatus(), status);

      return paymentStatusUpdater.updateWithExternalPaymentDetails(danglingPayment,
          externalPaymentDetails);
    } finally {
      long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      log.info("Processing the dangling payment '{}' took {}ms", danglingPayment.getId(), elapsed);
    }
  }

  /**
   * Verifies whether passed {@code danglingPayment} is in valid state when calling {@link
   * CleanupDanglingPaymentService#processDanglingPayment(uk.gov.caz.psr.model.Payment)}.
   */
  private void checkPreconditions(Payment danglingPayment) {
    Preconditions.checkNotNull(danglingPayment, "Payment cannot be null");
    Preconditions.checkNotNull(danglingPayment.getExternalId(), "External id cannot be null");
    Preconditions.checkArgument(danglingPayment.getEntrantPayments().isEmpty(),
        "Entrant payments should be empty");
  }

  /**
   * For the passed {@code payment} it loads all associated vehicle entrant records. A new {@link
   * Payment} instance is created with the newly fetched records and all previous attributes from
   * {@code payment}.
   */
  private Payment loadEntrantPayments(Payment payment) {
    return payment.toBuilder()
        .entrantPayments(entrantPaymentRepository.findByPaymentId(payment.getId()))
        .build();
  }

  /**
   * Retrieves the Clean Air Zone ID for the given {@link Payment}.
   *
   * @param payment an instance of a {@link Payment} object
   * @return a {@link UUID} representing a Clean Air Zone.
   */
  private UUID getCleanAirZoneId(Payment payment) {
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Vehicle entrant payments should not be empty");
    return payment.getEntrantPayments().iterator().next().getCleanAirZoneId();
  }
}
