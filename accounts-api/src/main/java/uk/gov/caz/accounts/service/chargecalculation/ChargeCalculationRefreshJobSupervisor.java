package uk.gov.caz.accounts.service.chargecalculation;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.service.ChargeCalculationService;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;

/**
 * Supervisor of {@link ChargeCalculationService#refreshCache(int, int)} executions. Manages state
 * of the registered job accordingly.
 */
@Component
@Slf4j
public class ChargeCalculationRefreshJobSupervisor {

  private final ChargeCalculationService chargeCalculationService;
  private final AsyncChargeCalculationRefreshStarter asyncChargeCalculationRefreshStarter;

  private final int chargeabilityCacheRefreshDays;
  private final int maxInvocationCount;
  private final int maxVehiclesToProcess;

  /**
   * Creates an instance of this class.
   */
  public ChargeCalculationRefreshJobSupervisor(
      ChargeCalculationService chargeCalculationService,
      AsyncChargeCalculationRefreshStarter asyncChargeCalculationRefreshStarter,
      @Value("${charge-calculation.lambda.refresh-days:30}")
          int chargeabilityCacheRefreshDays,
      @Value("${charge-calculation.lambda.max-invocation-count:40}") int maxInvocationCount,
      @Value("${charge-calculation.lambda.max-vehicles-to-process:5000}")
          int maxVehiclesToProcess) {
    this.chargeCalculationService = chargeCalculationService;
    this.asyncChargeCalculationRefreshStarter = asyncChargeCalculationRefreshStarter;
    this.chargeabilityCacheRefreshDays = chargeabilityCacheRefreshDays;
    this.maxInvocationCount = maxInvocationCount;
    this.maxVehiclesToProcess = maxVehiclesToProcess;
  }

  /**
   * Coordinates charge cache refresh by invoking another lambda that processes another batch of
   * vehicles if necessary.
   */
  public void refreshChargeCalculationCache(int invocationNumber, UUID correlationId) {
    CachePopulationResult cachePopulationResult = chargeCalculationService
        .refreshCache(maxVehiclesToProcess, chargeabilityCacheRefreshDays);
    if (cachePopulationResult == CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED) {
      processAnotherBatchIfApplicable(invocationNumber, correlationId);
    } else {
      finishCalculationJob(cachePopulationResult);
    }
  }

  /**
   * Finishes the whole task of processing the charge calculation by setting the appropriate job
   * status if applicable (i.e. when {@code jobId != null}).
   */
  private void finishCalculationJob(CachePopulationResult cachePopulationResult) {
    if (cachePopulationResult == CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION) {
      log.info("Failed to refresh charges.");
    } else {
      log.info("Refresh charges have been calculated for all vehicles in the system.");
    }
  }

  /**
   * If {@code invocationNumber < maxInvocationCount} a new async charge calculation is invoked with
   * {@code invocationNumber} set to {@code invocationNumber + 1}. Otherwise the job status is
   * updated to {@link RegisterJobStatus#FINISHED_FAILURE_MAX_INVOCATION_COUNT_REACHED}.
   */
  private void processAnotherBatchIfApplicable(int invocationNumber, UUID correlationId) {
    if (invocationNumber >= maxInvocationCount) {
      log.warn("Reached the maximum number of invocations: {}, stopping the calculation, marking",
          maxInvocationCount);
    } else {
      log.info("There are still vehicles for which charge calculations must be done. "
              + "Invoking Charge Calculation lambda again. "
              + "Invocations number: {} and the maximum number of invocations: {}",
          invocationNumber,
          maxInvocationCount);
      asyncChargeCalculationRefreshStarter.fireAndForget(
          correlationId,
          invocationNumber + 1
      );
    }
  }
}
