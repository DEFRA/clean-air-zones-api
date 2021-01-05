package uk.gov.caz.accounts.service.chargecalculation;

import java.util.UUID;

/**
 * Service that starts the fleet charge refresh.
 */
public interface AsyncChargeCalculationRefreshStarter {

  /**
   * Starts the refresh charge calculation in an asynchronous manner.
   */
  void fireAndForget(UUID correlationId, int newInvocationNumber);
}
