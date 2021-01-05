package uk.gov.caz.accounts.service.chargecalculation;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.caz.util.function.MdcAwareSupplier;

@Component
@Profile("development | integration-tests")
@RequiredArgsConstructor
@Slf4j
public class AsyncJavaBackgroundJobChargeCalculationRefreshStarter implements
    AsyncChargeCalculationRefreshStarter {

  // this is a hack for dev/integration test envs to avoid a circular reference
  // when creating this class and injecting ChargeCalculationJobSupervisor
  private final ApplicationContext applicationContext;

  /**
   * Executes the charge-cache-refresh logic in a background thread.
   */
  @Override
  public void fireAndForget(UUID correlationId, int newInvocationNumber) {
    logCallDetails(newInvocationNumber, correlationId);
    CompletableFuture.supplyAsync(refreshCacheTask(correlationId, newInvocationNumber));
  }

  private Supplier<Void> refreshCacheTask(UUID correlationId,
      int newInvocationNumber) {
    Supplier<Void> task = () -> {
      chargeCalculationRefreshJobSupervisor()
          .refreshChargeCalculationCache(newInvocationNumber, correlationId);
      return null;
    };

    return MdcAwareSupplier.from(task);
  }

  private void logCallDetails(int newInvocationNumber, UUID correlationId) {
    log.info("Starting Async, fire and forget, charge calculation refresh job with parameters: "
            + "newInvocationNumber: {}, Correlation: {} and runner "
            + "implementation: {}",
        newInvocationNumber, correlationId,
        AsyncJavaBackgroundJobChargeCalculationRefreshStarter.class.getSimpleName()
    );
  }

  private ChargeCalculationRefreshJobSupervisor chargeCalculationRefreshJobSupervisor() {
    return applicationContext.getBean(ChargeCalculationRefreshJobSupervisor.class);
  }
}
