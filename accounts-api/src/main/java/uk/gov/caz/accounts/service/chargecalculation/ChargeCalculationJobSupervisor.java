package uk.gov.caz.accounts.service.chargecalculation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.service.ChargeCalculationService;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.emailnotifications.ChargeCalculationCompleteEmailSender;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;

/**
 * Supervisor of {@link ChargeCalculationService#populateCache(java.util.UUID, int)} executions.
 * Manages state of the registered job accordingly.
 */
@Component
@Slf4j
public class ChargeCalculationJobSupervisor {
  private final ChargeCalculationService chargeCalculationService;
  private final AsyncChargeCalculationStarter asyncChargeCalculationStarter;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final ChargeCalculationCompleteEmailSender chargeCalculationCompleteEmailSender;
  private final UserService userService;

  private final int maxInvocationCount;
  private final int maxVehiclesToProcess;

  /**
   * Creates an instance of this class.
   */
  public ChargeCalculationJobSupervisor(
      ChargeCalculationService chargeCalculationService,
      AsyncChargeCalculationStarter asyncChargeCalculationStarter,
      RegisterJobSupervisor registerJobSupervisor,
      ChargeCalculationCompleteEmailSender chargeCalculationCompleteEmailSender,
      UserService userService,
      @Value("${charge-calculation.lambda.max-invocation-count:40}") int maxInvocationCount,
      @Value("${charge-calculation.lambda.max-vehicles-to-process:5000}")
          int maxVehiclesToProcess) {
    this.chargeCalculationService = chargeCalculationService;
    this.asyncChargeCalculationStarter = asyncChargeCalculationStarter;
    this.registerJobSupervisor = registerJobSupervisor;
    this.chargeCalculationCompleteEmailSender = chargeCalculationCompleteEmailSender;
    this.userService = userService;
    this.maxInvocationCount = maxInvocationCount;
    this.maxVehiclesToProcess = maxVehiclesToProcess;
  }

  /**
   * Coordinates charge calculation cache by invoking another lambda that processes another
   * batch of vehicles if necessary. Additionally emails are sent upon the successful completion
   * of the process. There is also a mechanism that prevents from
   */
  public void populateChargeCalculationCache(UUID accountId, Integer jobId,
      int invocationNumber, UUID correlationId,
      boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    CachePopulationResult cachePopulationResult = chargeCalculationService.populateCache(accountId,
        maxVehiclesToProcess);
    if (cachePopulationResult == CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED) {
      processAnotherBatchIfApplicable(accountId, jobId, invocationNumber, correlationId,
          shouldSendEmailsUponSuccessfulJobCompletion);
    } else {
      finishCalculationJob(cachePopulationResult, accountId, jobId,
          shouldSendEmailsUponSuccessfulJobCompletion);
    }
  }

  /**
   * Finishes the whole task of processing the charge calculation by setting the appropriate job
   * status if applicable (i.e. when {@code jobId != null}).
   */
  private void finishCalculationJob(CachePopulationResult cachePopulationResult, UUID accountId,
      Integer jobId, boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    if (Objects.isNull(jobId)) {
      log.info("Register job was not running for calculating charges for all vehicles in the fleet "
          + "with ID: {}, skipping sending emails", accountId);
      return;
    }

    if (cachePopulationResult == CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION) {
      log.info("Failed to calculate charges for fleet with ID: {}, job with ID: {} will be "
          + "marked as failed.", accountId, jobId);
      registerJobSupervisor.updateStatus(jobId, RegisterJobStatus.UNKNOWN_FAILURE);
    } else {
      log.info("Calculation charges have been calculated for all vehicles in the fleet with ID: "
          + "{}. Register job with ID: {} will be marked as finished.", accountId, jobId);
      if (shouldSendEmailsUponSuccessfulJobCompletion) {
        sendEmailsToAccountUsers(accountId);
      } else {
        log.info("Skipping sending cache-population-success emails to account users");
      }
      registerJobSupervisor.updateStatus(jobId, RegisterJobStatus.FINISHED_SUCCESS);
    }
  }

  /**
   * If {@code invocationNumber < maxInvocationCount} a new async charge calculation is invoked
   * with {@code invocationNumber} set to {@code invocationNumber + 1}. Otherwise the job status
   * is updated to {@link RegisterJobStatus#FINISHED_FAILURE_MAX_INVOCATION_COUNT_REACHED}.
   */
  private void processAnotherBatchIfApplicable(UUID accountId, Integer jobId, int invocationNumber,
      UUID correlationId, boolean shouldSendEmailsUponSuccessfulJobCompletion) {
    if (invocationNumber >= maxInvocationCount) {
      log.warn("Reached the maximum number of invocations: {}, stopping the calculation, marking "
          + "the job with ID: {} as failed", maxInvocationCount, jobId);
      registerJobSupervisor.updateStatus(jobId,
          RegisterJobStatus.FINISHED_FAILURE_MAX_INVOCATION_COUNT_REACHED);
    } else {
      log.info("There are still vehicles for which charge calculations must be done. "
              + "Invoking Charge Calculation lambda again. "
              + "Invocations number: {} and the maximum number of invocations: {}",
          invocationNumber,
          maxInvocationCount);
      asyncChargeCalculationStarter.fireAndForget(
          accountId,
          jobId,
          correlationId,
          invocationNumber + 1,
          shouldSendEmailsUponSuccessfulJobCompletion
      );
    }
  }

  /**
   * Method used to send email confirmations to all users associated to an account.
   *
   * @param accountId Account UUID
   */
  private void sendEmailsToAccountUsers(UUID accountId) {
    List<UserEntity> users = userService.getAllUsersForAccountId(accountId).stream()
        .filter(user -> !user.isRemoved() && user.hasVehicleManagementPermission())
        .collect(Collectors.toList());

    users.forEach(user -> chargeCalculationCompleteEmailSender.send(user.getEmail()));

    log.info("Successfully sent email(s) to {} users", users.size());
  }

}
