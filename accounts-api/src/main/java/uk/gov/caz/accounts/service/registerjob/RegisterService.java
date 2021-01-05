package uk.gov.caz.accounts.service.registerjob;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;

/**
 * Class which is responsible for registering vehicles. It wipes all vehicles before persisting new
 * ones.
 */
@Service
@Slf4j
@AllArgsConstructor
public class RegisterService {

  private static final int DELETE_BATCH_SIZE = 10_000;
  private final AccountVehicleRepository accountVehicleRepository;

  /**
   * Registers the passed sets of {@link AccountVehicle}.
   *
   * @param newAccountVehicles a set of {@link AccountVehicle} which insert to account.
   * @return An instance of {@link RegisterResult} that represents the result of the operation.
   */
  @Transactional
  public RegisterResult register(Set<AccountVehicle> newAccountVehicles, UUID accountId) {
    Preconditions.checkNotNull(newAccountVehicles,
        "'newAccountVehicles' cannot be null");
    Preconditions.checkArgument(!newAccountVehicles.isEmpty(),
        "'newAccountVehicles' cannot be empty");
    Preconditions.checkArgument(verifySameAccountId(newAccountVehicles, accountId),
        "AccountIds are not the same for each AccountVehicle");
    log.info("Registering {} AccountVehicle(s) for Account {} : start", newAccountVehicles.size(),
        accountId);
    List<AccountVehicle> vehiclesExisitingInDb = accountVehicleRepository
        .findAllByAccountId(accountId, Pageable.unpaged())
        .getContent();
    Set<String> vrnExisting = vrnsFromAccountVehicles(vehiclesExisitingInDb);
    log.info("We already have {} vehicles.", vrnExisting.size());
    Set<String> vrnsFromCsv = vrnsFromAccountVehicles(newAccountVehicles);
    log.info("We received {} new vehicles.", vrnsFromCsv.size());
    deleteVehiclesMissingInNewSet(vehiclesExisitingInDb, vrnsFromCsv);
    saveNewVehiclesAddedInSet(newAccountVehicles, vrnsFromCsv, vrnExisting);
    log.info("Registering {} AccountVehicle(s) for Account {} : finish", newAccountVehicles.size(),
        accountId);
    return RegisterResult.success();
  }

  /**
   * Helper method which save only new AccountVehicles in DB.
   * @param newAccountVehicles {@link Set} AccountVehicles from CSV.
   * @param vrnsFromCsv {@link Set} of String VRNs from CSV.
   * @param vrnExisting {@link Set} of String VRNs from Database.
   */
  private void saveNewVehiclesAddedInSet(Set<AccountVehicle> newAccountVehicles,
      Set<String> vrnsFromCsv, Set<String> vrnExisting) {
    Set<String> newVrns = Sets.difference(vrnsFromCsv, vrnExisting);
    log.info("There are {} vehicles to be added.", newVrns.size());
    Set<AccountVehicle> newEntities = newAccountVehicles.stream()
        .filter(vehicle -> newVrns.contains(vehicle.getVrn()))
        .collect(Collectors.toSet());
    if (!newVrns.isEmpty()) {
      accountVehicleRepository.saveAll(newEntities);
    }
  }

  /**
   * Method mapping {@link AccountVehicle} into its String vrn.
   * @param vehicles collection of {@link AccountVehicle}
   * @return set of vrns
   */
  private Set<String> vrnsFromAccountVehicles(Collection<AccountVehicle> vehicles) {
    return vehicles.stream()
        .map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
  }

  /**
   * Helper method which delete AccountVehicles that were removed in CSV.
   * @param vehiclesExistingInDb {@link List} of existing vehicles from DB.
   * @param newAccountVehiclesVrns {@link Set} of VRNs from vehicles that need to present in the DB.
   */
  private void deleteVehiclesMissingInNewSet(List<AccountVehicle> vehiclesExistingInDb,
      Set<String> newAccountVehiclesVrns) {
    List<AccountVehicle> toBeDeleted = vehiclesExistingInDb.stream()
        .filter(existingVehicle -> !newAccountVehiclesVrns.contains(existingVehicle.getVrn()))
        .collect(Collectors.toList());
    log.info("There are {} vehicles to be removed.", toBeDeleted.size());
    if (!toBeDeleted.isEmpty()) {
      deleteVehiclesInBatches(toBeDeleted);
    }
  }

  /**
   * Deletes vehicles in batches.
   * @param toBeDeleted all vehicles that are to be removed
   */
  private void deleteVehiclesInBatches(List<AccountVehicle> toBeDeleted) {
    Iterable<List<AccountVehicle>> batches = Iterables.partition(toBeDeleted, DELETE_BATCH_SIZE);
    batches.forEach(accountVehicleRepository::deleteAll);
  }

  /**
   * Verifies if all {@code AccountVehicle} accountIds are the same.
   */
  private boolean verifySameAccountId(Set<AccountVehicle> accountVehicles, UUID accountId) {
    return accountVehicles.stream()
        .allMatch(accountVehicle -> accountVehicle.getAccountId().equals(accountId));
  }
}
