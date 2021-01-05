package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import retrofit2.Response;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.model.VehiclesToCalculateChargeability;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.VccsRepository;
import uk.gov.caz.accounts.repository.VehicleChargeabilityRepository;
import uk.gov.caz.accounts.service.exception.ExternalServiceCallException;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

/**
 * Service that allows to calculate compliance results for fleet vehicles.
 */
@Service
@Slf4j
public class ChargeCalculationService {

  private final VehicleChargeabilityRepository vehicleChargeabilityRepository;
  private final AccountVehicleRepository accountVehicleRepository;
  private final VccsRepository vccsRepository;
  private final int vccsBulkCheckBatchSize;

  /**
   * Initializes new instance of {@link ChargeCalculationService} class.
   *
   * @param vehicleChargeabilityRepository JPA Repository over {@link VehicleChargeability}
   *     entities.
   * @param accountVehicleRepository JPA Repository over {@link AccountVehicle} entities.
   * @param vccsRepository REST client to query VCCS service.
   * @param vccsBulkCheckBatchSize Count of VRNs that will be used in bulk compliance
   *     calculation in VCCS.
   */
  public ChargeCalculationService(
      VehicleChargeabilityRepository vehicleChargeabilityRepository,
      AccountVehicleRepository accountVehicleRepository,
      VccsRepository vccsRepository,
      @Value("${charge-calculation.vccs.bulk-check-batch-size:10}") int vccsBulkCheckBatchSize) {
    this.vehicleChargeabilityRepository = vehicleChargeabilityRepository;
    this.accountVehicleRepository = accountVehicleRepository;
    this.vccsRepository = vccsRepository;
    this.vccsBulkCheckBatchSize = vccsBulkCheckBatchSize;
  }

  /**
   * Result from {@code populateCache} method.
   */
  public enum CachePopulationResult {
    /**
     * Batch was processed but there are still vehicles that do not have charge calculations
     * cached.
     */
    PROCESSED_BATCH_BUT_STILL_NOT_FINISHED,
    /**
     * Batch was processed and there are no more vehicles that need to have charge calculations
     * cached.
     */
    ALL_RECORDS_CACHED,

    /**
     * There was an error during processing of the bach - a call to an external service failed.
     */
    EXTERNAL_SERVICE_CALL_EXCEPTION;
  }

  /**
   * Will fetch compliance results and update chargeability cache for {@code maxVehiclesToProcess}
   * in the fleet.
   *
   * @param accountId Fleet/Account ID.
   * @param maxVehiclesToProcess limit of how many vehicles to process. 0 or negative number to
   *     not use any limit and try to process all vehicles in one go.
   * @return {@link CachePopulationResult} which states whether there is still more work or if all
   *     vehicles in the fleet have been processed.
   */
  @Transactional
  public CachePopulationResult populateCache(UUID accountId, int maxVehiclesToProcess) {
    try {
      Set<UUID> allCazesIds = fetchAllCazesIds();
      VehiclesToCalculateChargeability allVehiclesToProcess = allAccountVehiclesToProcess(accountId,
          count(allCazesIds));

      if (allVehiclesToProcess.setOfIDs().isEmpty()) {
        return CachePopulationResult.ALL_RECORDS_CACHED;
      }

      VehiclesToCalculateChargeability subsetOfVehiclesToProcess = allVehiclesToProcess;
      if (maxVehiclesToProcess > 0) {
        subsetOfVehiclesToProcess = subset(maxVehiclesToProcess, allVehiclesToProcess);
      }
      calculateChargeabilityCacheFor(subsetOfVehiclesToProcess, allCazesIds);
      return determineResult(maxVehiclesToProcess, allVehiclesToProcess);
    } catch (ExternalServiceCallException e) {
      log.error("ExternalServiceCallException - returning {} as a result. Exception: ",
          CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION, e);
      return CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION;
    }
  }

  /**
   * Will fetch compliance results and update chargeability cache for {@code maxVehiclesToProcess}
   * in the fleet.
   *
   * @param maxVehiclesToProcess limit of how many vehicles to process. 0 or negative number to
   *     not use any limit and try to process all vehicles in one go.
   * @return {@link CachePopulationResult} which states whether there is still more work or if all
   *     vehicles in the fleet have been processed.
   */
  @Transactional
  public CachePopulationResult refreshCache(int maxVehiclesToProcess,
      int chargeabilityCacheRefreshDays) {
    try {
      Set<UUID> allCazesIds = fetchAllCazesIds();
      VehiclesToCalculateChargeability allVehiclesToProcess = allVehiclesToProcess(
          count(allCazesIds), chargeabilityCacheRefreshDays);

      if (allVehiclesToProcess.setOfIDs().isEmpty()) {
        return CachePopulationResult.ALL_RECORDS_CACHED;
      }

      VehiclesToCalculateChargeability subsetOfVehiclesToProcess = allVehiclesToProcess;
      if (maxVehiclesToProcess > 0) {
        subsetOfVehiclesToProcess = subset(maxVehiclesToProcess, allVehiclesToProcess);
      }
      calculateChargeabilityCacheFor(subsetOfVehiclesToProcess, allCazesIds);
      return determineResult(maxVehiclesToProcess, allVehiclesToProcess);
    } catch (ExternalServiceCallException e) {
      log.error("ExternalServiceCallException - returning {} as a result. Exception: ",
          CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION, e);
      return CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION;
    }
  }

  /**
   * Synchronously calculates chargeabitlity cache for specified vehicle in all CAZes. Will not
   * invoke any lambdas.
   *
   * @param accountVehicleId ID of account vehicle.
   * @param vrn VRN of vehicle.
   */
  @Transactional
  public void populateCacheForSingleVehicle(UUID accountVehicleId, String vrn) {
    VehiclesToCalculateChargeability vehicleToCalculateChargeability =
        VehiclesToCalculateChargeability.fromSingle(accountVehicleId, vrn);
    calculateChargeabilityCacheFor(vehicleToCalculateChargeability, fetchAllCazesIds());
  }

  /**
   * Calculates chargeability cache for all specified vehicles in all CAZes. Will delete any
   * incomplete previous charge calculations for these vehicles, update vehicle type in
   * AccountVehicle, and update chargeability cache table.
   */
  private void calculateChargeabilityCacheFor(
      VehiclesToCalculateChargeability subsetOfVehiclesToProcess,
      Set<UUID> allCazesIds) {
    deleteTheOnesThatWillBeFetched(subsetOfVehiclesToProcess);
    List<VehiclesToCalculateChargeability> batches = batchForVccsBulkComplianceCheck(
        subsetOfVehiclesToProcess);
    for (VehiclesToCalculateChargeability batch : batches) {
      callVccsThenUpdateChargeabilityCacheAndVehicleType(batch, allCazesIds);
    }
  }

  /**
   * Fetch all fleet vehicles that do not have chargeability cache populated in all CAZes.
   */
  private VehiclesToCalculateChargeability allAccountVehiclesToProcess(UUID accountId,
      int allCazesCount) {
    return vehicleChargeabilityRepository
        .findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(accountId, allCazesCount);
  }


  /**
   * Fetch all fleet vehicles that do not have chargeability cache populated in all CAZes.
   */
  private VehiclesToCalculateChargeability allVehiclesToProcess(int allCazesCount,
      int chargeabilityCacheRefreshDays) {
    Timestamp expiredRefreshTimestamp = Timestamp
        .valueOf(LocalDateTime.now().minusDays(chargeabilityCacheRefreshDays));

    return vehicleChargeabilityRepository
        .findAllThatDoNotHaveChargeabilityCacheInEachCazOrExpired(allCazesCount,
            expiredRefreshTimestamp);
  }

  /**
   * Extract subset from list of all vehicles to update, limited to specified number.
   */
  private VehiclesToCalculateChargeability subset(int maxVehiclesToProcess,
      VehiclesToCalculateChargeability allVehiclesToProcess) {
    return allVehiclesToProcess
        .subsetLimitedTo(maxVehiclesToProcess);
  }

  /**
   * Remove all entries from T_VEHICLE_CHARGEABILITY that will be updated in the current
   * batch/subset processing.
   */
  private void deleteTheOnesThatWillBeFetched(
      VehiclesToCalculateChargeability subsetOfVehiclesToProcess) {
    vehicleChargeabilityRepository
        .deleteFromVehicleChargeability(subsetOfVehiclesToProcess.setOfIDs());
  }

  /**
   * Partitions subset of vehicles to be updated into small batches that can be used in VCCS bulk
   * compliance calls.
   */
  private List<VehiclesToCalculateChargeability> batchForVccsBulkComplianceCheck(
      VehiclesToCalculateChargeability subsetOfVehiclesToProcess) {
    return subsetOfVehiclesToProcess.toBatchesOfSize(vccsBulkCheckBatchSize);
  }

  /**
   * Use VCCS to fetch a set of IDs of all CAZes in the system.
   */
  private Set<UUID> fetchAllCazesIds() {
    return vccsRepository.findCleanAirZonesSync().body()
        .getCleanAirZones()
        .stream()
        .map(CleanAirZoneDto::getCleanAirZoneId)
        .collect(Collectors.toSet());
  }

  /**
   * Give count of CAZes in the system.
   */
  private <T> int count(Collection<T> allCazes) {
    return allCazes.size();
  }

  /**
   * Given batch of vehicles for which to calc chargeability, will call VCCS to bulk check them and
   * then update T_VEHICLE_CHARGEABILITY table with results and as a bonus update vehicle type in
   * T_ACCOUNT_VEHICLE as it will be returned from VCCS.
   */
  @SneakyThrows
  private void callVccsThenUpdateChargeabilityCacheAndVehicleType(
      VehiclesToCalculateChargeability batch, Set<UUID> allCazesIds) {
    Response<List<ComplianceResultsDto>> compliance = vccsRepository
        .findComplianceInBulkSync(batch.setOfVrns());
    if (!compliance.isSuccessful()) {
      String errorMessage = compliance.errorBody().string();
      log.error("VCCS bulk checking error when populating chargeability cache: {} - {}",
          compliance.code(), errorMessage);
      throw new ExternalServiceCallException(errorMessage);
    }

    List<VehicleChargeability> entitiesToSave = newArrayList();
    for (ComplianceResultsDto complianceResultsDto : compliance.body()) {
      String vrn = complianceResultsDto.getRegistrationNumber();
      UUID accountVehicleId = batch.getAccountVehicleIdFor(vrn);
      updateVehicleTypeIfNotNull(complianceResultsDto, accountVehicleId);
      List<VehicleChargeability> chargeabilityEntities = createChargeabilityEntities(allCazesIds,
          complianceResultsDto, accountVehicleId);
      entitiesToSave.addAll(chargeabilityEntities);
    }
    vehicleChargeabilityRepository.saveAll(entitiesToSave);
  }

  private List<VehicleChargeability> createChargeabilityEntities(Set<UUID> allCazesIds,
      ComplianceResultsDto complianceResultsDto, UUID accountVehicleId) {
    if (isNonCompliantVehicle(complianceResultsDto)) {
      return allCazesIds.stream()
          .map(cazId -> buildNonCompliantVehicleChargeabilityEntity(cazId, accountVehicleId))
          .collect(Collectors.toList());
    }

    // vehicle is compliant <=> `ComplianceResultsDto.getComplianceOutcomes` is not empty
    return complianceResultsDto.getComplianceOutcomes()
        .stream()
        .map(complianceOutcomeDto -> buildVehicleChargeabilityEntity(complianceResultsDto,
            accountVehicleId, complianceOutcomeDto))
        .collect(Collectors.toList());
  }

  private VehicleChargeability buildNonCompliantVehicleChargeabilityEntity(UUID cazId,
      UUID accountVehicleId) {
    return VehicleChargeability.builder()
        .accountVehicleId(accountVehicleId)
        .cazId(cazId)
        .charge(null)
        .isExempt(false)
        .isRetrofitted(false)
        .tariffCode(null)
        .build();
  }

  private boolean isNonCompliantVehicle(ComplianceResultsDto complianceResultsDto) {
    return CollectionUtils.isEmpty(complianceResultsDto.getComplianceOutcomes());
  }

  /**
   * VCCS provides vehicle type and it is a good moment to update it in T_ACCOUNT_VEHICLE.
   */
  private void updateVehicleTypeIfNotNull(ComplianceResultsDto complianceResultsDto,
      UUID accountVehicleId) {
    String vehicleType = complianceResultsDto.getVehicleType();
    if (vehicleType != null) {
      accountVehicleRepository.updateVehicleType(accountVehicleId, vehicleType);
    }
  }

  /**
   * Build new instance of {@link VehicleChargeability}.
   */
  private VehicleChargeability buildVehicleChargeabilityEntity(
      ComplianceResultsDto complianceResultsDto,
      UUID accountVehicleId,
      ComplianceOutcomeDto complianceOutcomeDto) {
    return VehicleChargeability.builder()
        .accountVehicleId(accountVehicleId)
        .cazId(complianceOutcomeDto.getCleanAirZoneId())
        .charge(BigDecimal.valueOf(complianceOutcomeDto.getCharge()))
        .isExempt(complianceResultsDto.getIsExempt())
        .isRetrofitted(complianceResultsDto.getIsRetrofitted())
        .tariffCode(complianceOutcomeDto.getTariffCode())
        .build();
  }

  /**
   * Will determine whether there is still more work or if all vehicles in the fleet have been
   * processed.
   */
  private CachePopulationResult determineResult(int maxRecordsToProcess,
      VehiclesToCalculateChargeability allVehiclesToProcess) {
    return (maxRecordsToProcess > 0) && (allVehiclesToProcess.size() > maxRecordsToProcess)
        ? CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED
        : CachePopulationResult.ALL_RECORDS_CACHED;
  }
}