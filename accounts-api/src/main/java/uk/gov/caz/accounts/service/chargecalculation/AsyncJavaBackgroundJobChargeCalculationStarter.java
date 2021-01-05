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
public class AsyncJavaBackgroundJobChargeCalculationStarter implements
    AsyncChargeCalculationStarter {

  // this is a hack for dev/integration test envs to avoid a circular reference
  // when creating this class and injecting ChargeCalculationJobSupervisor
  private final ApplicationContext applicationContext;

  /**
   * Executes the charge-population-cache logic in a background thread.
   */
  @Override
  public void fireAndForget(UUID accountId, Integer jobId, UUID correlationId,
      int newInvocationNumber, boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    logCallDetails(accountId, jobId, newInvocationNumber, correlationId);
    CompletableFuture.supplyAsync(populateCacheTask(accountId, jobId, correlationId,
        newInvocationNumber, shouldSendEmailsUponSuccessfulJobCompletion));
  }

  private Supplier<Void> populateCacheTask(UUID accountId, Integer jobId, UUID correlationId,
      int newInvocationNumber, boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    Supplier<Void> task = () -> {
      chargeCalculationJobSupervisor()
          .populateChargeCalculationCache(accountId, jobId, newInvocationNumber, correlationId,
              shouldSendEmailsUponSuccessfulJobCompletion);
      return null;
    };

    return MdcAwareSupplier.from(task);
  }

  private void logCallDetails(UUID accountId, Integer jobId, int newInvocationNumber,
      UUID correlationId) {
    log.info("Starting Async, fire and forget, charge calculation job with parameters: "
            + "accountId: {}, jobId: {}, newInvocationNumber: {}, Correlation: {} and runner "
            + "implementation: {}",
        accountId, jobId, newInvocationNumber, correlationId,
        AsyncJavaBackgroundJobChargeCalculationStarter.class.getSimpleName()
    );
  }

  private ChargeCalculationJobSupervisor chargeCalculationJobSupervisor() {
    return applicationContext.getBean(ChargeCalculationJobSupervisor.class);
  }
}
