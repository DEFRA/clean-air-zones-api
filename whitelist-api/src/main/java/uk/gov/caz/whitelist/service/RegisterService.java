package uk.gov.caz.whitelist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.AuditingRepository;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;

/**
 * Class which is responsible for registering vehicles. It wipes all vehicles before persisting new
 * ones.
 */
@Service
@AllArgsConstructor
@Slf4j
public class RegisterService {

  private static final int DELETE_BATCH_SIZE = 1000;

  private final WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;
  private final AuditingRepository auditingRepository;

  /**
   * Registers the passed sets of {@link WhitelistVehicle}.
   *
   * @param conversionResults An object of {@link ConversionResults} which contains set of
   *     vehicles to insert, update and delete.
   * @return An instance of {@link RegisterResult} that represents the result of the operation.
   */
  @Transactional
  public RegisterResult register(ConversionResults conversionResults, UUID uploaderId,
      String uploaderEmail) {
    Preconditions.checkNotNull(conversionResults,
        "ConversionResults cannot be null");
    Preconditions.checkNotNull(uploaderId, "uploaderId cannot be null");

    log.info("Registering {} vehicle(s) : start", conversionResults.size());

    auditingRepository.tagModificationsInCurrentTransactionBy(uploaderId, uploaderEmail);
    log.info("Transaction associated with {} in the audit table.", uploaderId);

    saveOrUpdate(conversionResults.getWhitelistVehiclesToSaveOrUpdate());
    deleteVehicles(conversionResults.getVrnToDelete());

    log.info("Registering {} vehicle(s) : finish", conversionResults.size());
    return RegisterResult.success();
  }

  /**
   * Helper method which save or update vehicles for non empty set.
   *
   * @param whitelistVehicleToSaveOrUpdate {@link Set} vehicles to save or update.
   */
  private void saveOrUpdate(Set<WhitelistVehicle> whitelistVehicleToSaveOrUpdate) {
    if (whitelistVehicleToSaveOrUpdate.isEmpty()) {
      log.info("No vehicles to insert");
      return;
    }
    log.info("Inserting or updating {} whitelist vehicles", whitelistVehicleToSaveOrUpdate.size());
    whitelistVehiclePostgresRepository.saveOrUpdate(whitelistVehicleToSaveOrUpdate);
  }

  /**
   * Helper method which deletes vehicles for non empty set.
   *
   * @param vrnToDelete {@link Set} vehicles to delete.
   */
  private void deleteVehicles(Set<String> vrnToDelete) {
    if (vrnToDelete.isEmpty()) {
      log.info("No vehicles to delete");
      return;
    }
    Iterable<List<String>> batches = Iterables.partition(vrnToDelete, DELETE_BATCH_SIZE);
    for (List<String> batch : batches) {
      log.info("Deleting {} whitelist vehicles", batch.size());
      whitelistVehiclePostgresRepository.deleteByVrnsIn(batch);
    }
  }
}
