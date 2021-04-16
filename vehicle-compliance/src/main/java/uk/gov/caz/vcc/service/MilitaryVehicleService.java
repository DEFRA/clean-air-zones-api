package uk.gov.caz.vcc.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.dto.PreFetchedDataResults;
import uk.gov.caz.vcc.repository.ModDataProvider;

/**
 * Service layer for checking whether a vehicle is registered as a military vehicle.
 */
@Slf4j
@Service
public class MilitaryVehicleService {

  private final ModDataProvider militaryVehicleRepository;
  private final Function<Set<String>, Set<String>> filterMilitaryVrnsHandler;

  public MilitaryVehicleService(ModDataProvider militaryVehicleRepository,
      @Value("${services.mod.use-bulk-search-endpoint:false}") boolean useBulkSearchEndpoint) {
    this.militaryVehicleRepository = militaryVehicleRepository;
    this.filterMilitaryVrnsHandler = getFilterMilitaryVrnsHandler(useBulkSearchEndpoint);
  }

  /**
   * Checks whether a vehicle has been registered as a military vehicle.
   *
   * @param vrn the registration number of the vehicle to check.
   * @return boolean indicator of whether the vehicle has been registered as a military vehicle.
   */
  public boolean isMilitaryVehicle(String vrn) {
    // Note that the outcome of this check is not captured
    // to avoid this data being exposed via log files.
    return militaryVehicleRepository.existsByVrnIgnoreCase(vrn);
  }

  /**
   * Reduces a list of quoted VRNs to only those which are matched to military vehicles.
   *
   * @param vrns a series of unfiltered VRNs for inspection.
   * @return a filtered list of VRNs comprising only of military VRNs.
   */
  public Set<String> filterMilitaryVrnsFromList(Set<String> vrns) {
    return filterMilitaryVrnsHandler.apply(vrns);
  }

  public boolean isMilitaryVrn(String vrn,
      PreFetchedDataResults preFetchedDataResults) {
    return preFetchedDataResults.getMatchedMilitaryVrns().contains(vrn);
  }
  
  /**
   * Extract military vehicles.
   *
   * @param vrns Vrn list.
   * @param latch Synchronization controller
   * @param timeout Function execution timeout
   * @return An object of {@link MilitaryVehicleService.MilitaryVehicleQueryResponse}.
   */
  public MilitaryVehicleQueryResponse extractMilitaryVehiclesOutOf(
      List<String> vrns, CountDownLatch latch, long timeout) {

    final String ExceptionFormatter = "Mod query exception {}";
    List<String> militaryVehicleVrns = new ArrayList<>();
    List<String> unProcessedVrns = new ArrayList<>();
    List<Callable<MilitaryVehicleCheckResult>> taskes = new ArrayList<>(vrns.size());
    ExecutorService executor = Executors.newWorkStealingPool();

    for (String vrn : vrns) {
      taskes.add(() -> {
        try {
          boolean isModVehicle = isMilitaryVehicle(vrn);
          return new MilitaryVehicleCheckResult(vrn, isModVehicle);
        } catch (Exception ex) {
          log.error(ExceptionFormatter, ex.getMessage());
          return new MilitaryVehicleCheckResult(vrn, null);
        }
      });
    }

    try {
      List<Future<MilitaryVehicleCheckResult>> futureResponse =
          executor.invokeAll(taskes, timeout, TimeUnit.SECONDS);
      futureResponse.stream().forEach(fs -> {
        try {
          MilitaryVehicleCheckResult checkResult = fs.get();
          String vrn = checkResult.getVrn();
          if (checkResult.getIsAnMod() == null) {
            unProcessedVrns.add(vrn);
          } else if (checkResult.getIsAnMod()) {
            militaryVehicleVrns.add(vrn);
          }
        } catch (Exception e) {
          log.error(ExceptionFormatter, e.getMessage());
        }
      });
    } catch (InterruptedException e) {
      log.error(ExceptionFormatter, e.getMessage());
      unProcessedVrns.addAll(vrns.stream().collect(Collectors.toList()));
      Thread.currentThread().interrupt();
    } finally {
      if (latch != null) {
        latch.countDown();
      }
    }
    return new MilitaryVehicleQueryResponse(militaryVehicleVrns, unProcessedVrns);
  }

  /**
   * Determines whether a bulk or sequential version of {@code filterMilitaryVrnsFromList} should
   * be used.
   */
  private Function<Set<String>, Set<String>> getFilterMilitaryVrnsHandler(
      boolean useBulkSearchEndpoint) {
    if (useBulkSearchEndpoint) {
      log.info("Using MOD bulk search endpoint");
      return this::bulkFilterMilitaryVrnsFromList;
    }
    return this::sequentiallyFilterMilitaryVrnsFromList;
  }

  /**
   * A sequential variant of {@link this#filterMilitaryVrnsFromList(Set)}.
   */
  private Set<String> sequentiallyFilterMilitaryVrnsFromList(Set<String> vrns) {
    return vrns.stream()
        .filter(militaryVehicleRepository::existsByVrnIgnoreCase)
        .collect(Collectors.toSet());
  }

  /**
   * A bulk variant of {@link MilitaryVehicleService#filterMilitaryVrnsFromList(Set)}.
   */
  private Set<String> bulkFilterMilitaryVrnsFromList(Set<String> vrns) {
    Map<String, Boolean> existByVrns = militaryVehicleRepository.existByVrns(vrns);
    return vrns.stream()
        .filter(vrnToCheck -> existByVrns.getOrDefault(vrnToCheck, Boolean.FALSE))
        .collect(Collectors.toSet());
  }

  @RequiredArgsConstructor
  @Getter
  public static class MilitaryVehicleQueryResponse {

    private final List<String> militaryVehicleVrns;
    private final List<String> unProcessedVrns;
  }

  @RequiredArgsConstructor
  @Getter
  static class MilitaryVehicleCheckResult {

    private final String vrn;
    private final Boolean isAnMod;
  }
}