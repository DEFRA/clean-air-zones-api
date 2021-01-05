package uk.gov.caz.accounts.service.chargecalculation;

import java.util.UUID;

/**
 * Service that starts the fleet charge calculation.
 */
public interface AsyncChargeCalculationStarter {

  /**
   * Starts the charge calculation in an asynchronous manner.
   */
  void fireAndForget(UUID accountId, Integer jobId, UUID correlationId,
      int newInvocationNumber, boolean shouldSendEmailsUponSuccessfulChargeCalculation);
}
