package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.VrmSet;
import uk.gov.caz.taxiregister.repository.AuditingRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;

/**
 * Class which is responsible for registering vehicles. It determines which vehicles are to be
 * inserted, updated or deleted in the database when they are being registered.
 */
@Service
@AllArgsConstructor
@Slf4j
public class RegisterService {

  private static final String MISMATCH_LICENSING_AUTHORITY_NAME_ERROR_MESSAGE_TEMPLATE = "Vehicle's"
      + " licensing authority %s does not match any existing ones.";

  private final TaxiPhvLicencePostgresRepository vehicleRepository;
  private final RegisterContextFactory registerContextFactory;
  private final VehicleComplianceCheckerService vehicleComplianceCheckerService;
  private final AuditingRepository auditingRepository;

  /**
   * Registers {@code licences} in the database for a given {@code uploaderId}.
   *
   * <p>For a given uploader id: - records which are present in the database, but absent in {@code
   * licences} will be deleted - records which are a) present in the database, b) present in {@code
   * licences} and c) CHANGED any of their attributes will be updated - records which are a) present
   * in the database, b) present in {@code licences} and c) did NOT change any of their attributes
   * will NOT be updated - records which are absent in the database, but present in {@code licences}
   * will be inserted</p>
   *
   * @param licences {@link Set} of linces {@link TaxiPhvVehicleLicence} which need to be
   *     registered
   * @param uploaderId An identifier the entity which registers {@code licences}
   */
  @Transactional
  public RegisterResult register(Set<TaxiPhvVehicleLicence> licences, UUID uploaderId) {
    Preconditions.checkNotNull(licences, "licences cannot be null");
    Preconditions.checkNotNull(uploaderId, "uploaderId cannot be null");
    // assertion: for every vehicle in 'taxiPhvVehicleLicences' id is null
    // assertion: for every vehicle in 'taxiPhvVehicleLicences' LicensingAuthority.id is null
    try {
      log.info("Registering {} licence(s) for uploader '{}' : start", licences.size(), uploaderId);

      Stopwatch timer = Stopwatch.createStarted();
      
      auditingRepository.tagModificationsInCurrentTransactionBy(uploaderId);

      RegisterContext context = registerContextFactory.createContext(licences, uploaderId);
      log.info("Registering context took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
      logAffectedLicensingAuthorities(context);
      timer.reset().start();
      VrmSet affectedVrms = registerLicencesForEachLicensingAuthorityAndCollectSetOfAffectedVrms(
          context);
      log.info("Register Licences For Each Licensing"
               + "Authority And Collect Set Of Affected Vrms took {}ms",
               timer.elapsed(TimeUnit.MILLISECONDS));
      refreshCache(affectedVrms);
      return SuccessRegisterResult.with(context.getNewLicencesByLicensingAuthority().keySet());
    } catch (LicensingAuthorityMismatchException exception) {
      return toFailure(exception);
    } finally {
      log.info("Registering {} licence(s) for uploader '{}' : finish", licences.size(), uploaderId);
    }
  }

  /**
   * Iterates all affected Licensing Authorities and for each registers changed (new, updated,
   * deleted) licences.
   *
   * @param context {@link RegisterContext} holding registration-related auxiliary attributes.
   * @return {@link VrmSet} with all VRMs that were changed (new, updated, deleted).
   */
  private VrmSet registerLicencesForEachLicensingAuthorityAndCollectSetOfAffectedVrms(
      RegisterContext context) {
    VrmSet allAffectedVrms = VrmSet.empty();
    for (Entry<LicensingAuthority, Set<TaxiPhvVehicleLicence>> entry : context
        .getNewLicencesByLicensingAuthority().entrySet()) {
      VrmSet affectedVrms = registerLicensesForLicensingAuthority(entry.getKey(), entry.getValue(),
          context);
      allAffectedVrms.union(affectedVrms);
    }
    return allAffectedVrms;
  }

  /**
   * Performs the registration (detecting records which are to be inserted, deleted and updated and
   * executing the required database operations) of the records for a given licensing authority.
   *
   * @param licensingAuthority The licensing authority whose records are to be registered.
   * @param licensingAuthorityLicences The licences of the {@code licensingAuthority} which are
   *     to be registered.
   * @param context Context of the operation (holder of helper attributes).
   * @return Set of VRMs that were somehow changed during registration (inserted, deleted or
   *     updated).
   */
  private VrmSet registerLicensesForLicensingAuthority(LicensingAuthority licensingAuthority,
      Set<TaxiPhvVehicleLicence> licensingAuthorityLicences, RegisterContext context) {
    // assertion: each licence in `licensingAuthorityLicences` has
    // `TaxiPhvVehicleLicence.licensingAuthority`equal to 'licensingAuthority'

    log.info("Handling registering {} licence(s) for {} : start",
        licensingAuthorityLicences.size(),
        licensingAuthority);

    Set<TaxiPhvVehicleLicence> toBeDeleted = computeToBeDeleted(licensingAuthorityLicences,
        licensingAuthority,
        context);
    Set<TaxiPhvVehicleLicence> toBeUpdated = computeToBeUpdated(licensingAuthorityLicences,
        licensingAuthority, context);
    Set<TaxiPhvVehicleLicence> toBeInserted = computeToBeInserted(licensingAuthorityLicences,
        licensingAuthority, context);

    log.info("The number of records to be inserted, updated and deleted respectively: {}/{}/{}",
        toBeInserted.size(), toBeUpdated.size(), toBeDeleted.size());

    vehicleRepository.delete(toSetOfIds(toBeDeleted));
    vehicleRepository.update(toBeUpdated);
    vehicleRepository.insert(toBeInserted);

    log.info("Handling registering {} licence(s) for {} : finish",
        licensingAuthorityLicences.size(),
        licensingAuthority);
    return VrmSet.from(toSetOfVrms(toBeDeleted))
        .union(toSetOfVrms(toBeInserted))
        .union(toSetOfVrms(toBeUpdated));
  }

  /**
   * Maps the exception to {@link FailureRegisterResult}.
   *
   * @param exception An exception indicating that some of the licensing authorities from the
   *     request are absent in the database.
   * @return {@link FailureRegisterResult} with a mismatch error.
   */
  private RegisterResult toFailure(LicensingAuthorityMismatchException exception) {
    List<ValidationError> errors = exception.getLicencesWithNonMatchingLicensingAuthority()
        .stream()
        .map(this::createLicensingAuthorityMismatchError)
        .collect(Collectors.toList());
    log.warn("Licensing authority name mismatch detected for {} records, aborting registration",
        errors.size());
    return FailureRegisterResult.with(errors);
  }

  /**
   * Helper method to refresh the caching tier based on an upload.
   *
   * @param affectedVrms Set of all VRMs affected by registration (inserted, deleted, updated).
   */
  private void refreshCache(VrmSet affectedVrms) {
    if (affectedVrms.getVrms().isEmpty()) {
      return;
    }
    log.info("Refreshing cache in NTR and VCCS: start");
    runAndSkipInCaseOfErrors("Refresh Cache", () -> {
      Stopwatch timer = Stopwatch.createStarted();
      evictLicencesFromNtrInternalCache();
      log.info("Evict Licences From Ntr Internal Cache took {}ms", 
               timer.elapsed(TimeUnit.MILLISECONDS));
      timer.reset().start();
      evictLicencesFromVccsCache(affectedVrms);
      log.info("Evict Licences From Vccs Cache took {}ms", 
               timer.elapsed(TimeUnit.MILLISECONDS));
    });
    log.info("Refreshing cache in NTR and VCCS: finish");
  }

  /**
   * Runs action(s) specified in @actions and if any of them throws any {@link Exception} just skips
   * operation. It won't break whole processing flow. Exception will be logged as error level.
   *
   * @param description Will be used as logging error statement.
   * @param actions {@link Runnable} with actions to run.
   */
  private void runAndSkipInCaseOfErrors(String description, Runnable actions) {
    try {
      actions.run();
    } catch (Exception e) {
      log.error(description + " operations encountered an error. Skipping.", e);
    }
  }

  /**
   * Helper method to refresh the internal NTR cache based on an upload.
   *
   * @param affectedVrms Set of all VRMs affected by registration (inserted, deleted, updated).
   */
  private void evictLicencesFromNtrInternalCache() {
    /*
     *  Note that for all items below, cache eviction is made.
     *  Whilst an update would seem appropriate for the updated and inserted records,
     *  the resulting object from the controller is an Optional of a vehicle, rather
     *  than a TaxiPhvVehicleLicence object. Therefore evictions are made
     *  and the cache is populated on first call to the controller for a given vrm.
     */
    log.info("Refreshing cache in NTR: start");
    vehicleRepository.cacheEvictLicensingRepository();
    log.info("Refreshing cache in NTR: finish");
  }

  /**
   * Helper method to refresh the VCCS cache based on an upload.
   *
   * @param affectedVrms Set of all VRMs affected by registration (inserted, deleted, updated).
   */
  private void evictLicencesFromVccsCache(VrmSet affectedVrms) {
    log.info("Refreshing cache in VCCS: start");
    vehicleComplianceCheckerService.purgeCacheOfNtrData(affectedVrms);
    log.info("Refreshing cache in VCCS: finish");
  }

  /**
   * Logs the information about the affected licensing authorities by this registration run.
   *
   * @param context Context of the registration.
   */
  private void logAffectedLicensingAuthorities(RegisterContext context) {
    log.info("Affected licensing authorities: {}",
        context.getNewLicencesByLicensingAuthority().keySet());
  }

  /**
   * Creates a validation error for a case when the licensing authority of the given licence is
   * absent in the database.
   *
   * @param licence A licence whose licensing authority is absent in the database.
   * @return {@link ValidationError} with the mismatch information.
   */
  private ValidationError createLicensingAuthorityMismatchError(TaxiPhvVehicleLicence licence) {
    return ValidationError.valueError(
        licence.getVrm(),
        String.format(MISMATCH_LICENSING_AUTHORITY_NAME_ERROR_MESSAGE_TEMPLATE,
            licence.getLicensingAuthority().getName())
    );
  }

  // ------ DELETE helper methods : begin

  /**
   * Determines which licences of a given licensing authority should be deleted.
   */
  private Set<TaxiPhvVehicleLicence> computeToBeDeleted(Set<TaxiPhvVehicleLicence> newLicences,
      LicensingAuthority licensingAuthority, RegisterContext context) {
    Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>> currentLicences =
        context.getCurrentLicencesByLicensingAuthority();
    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes =
        currentLicences.getOrDefault(licensingAuthority, Collections.emptyMap());

    // assertion:
    // 'currentLicencesByUniqueAttributes' is not null (can be empty)
    // 'newLicences' is not null and not empty

    Set<UniqueLicenceAttributes> toBeDeleted = computeToBeDeleted(
        toUniqueLicenceAttributesSet(newLicences),
        currentLicencesByUniqueAttributes.keySet()
    );

    return toBeDeleted.stream()
        .map(currentLicencesByUniqueAttributes::get)
        .collect(Collectors.toSet());
  }

  /**
   * Determines which licences should be deleted based on the sets of unique attributes of licences
   * which are present in the database and those which are to be registered.
   *
   * @param newLicencesByUniqueAttributes A set of licences which are to be registered.
   * @param currentLicencesByUniqueAttributes A set of licences which are present in the
   *     database.
   * @return A set of licences which should be deleted.
   */
  private Set<UniqueLicenceAttributes> computeToBeDeleted(
      Set<UniqueLicenceAttributes> newLicencesByUniqueAttributes,
      Set<UniqueLicenceAttributes> currentLicencesByUniqueAttributes) {
    return Sets.difference(currentLicencesByUniqueAttributes, newLicencesByUniqueAttributes);
  }

  /**
   * Maps a set of licences to a set of unique attributes of those licences.
   *
   * @param licences A set of {@link TaxiPhvVehicleLicence}.
   * @return A set of {@link UniqueLicenceAttributes} which has been mapped from {@code licences}.
   */
  private Set<UniqueLicenceAttributes> toUniqueLicenceAttributesSet(
      Set<TaxiPhvVehicleLicence> licences) {
    return licences.stream()
        .map(UniqueLicenceAttributes::from)
        .collect(Collectors.toSet());
  }

  //------ DELETE helper methods : end

  //------ UPDATE helper methods : begin

  /**
   * Determines which licences of a given licensing authority should be updated.
   */
  private Set<TaxiPhvVehicleLicence> computeToBeUpdated(Set<TaxiPhvVehicleLicence> newLicences,
      LicensingAuthority licensingAuthority, RegisterContext context) {
    Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>> currentLicences =
        context.getCurrentLicencesByLicensingAuthority();

    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes =
        currentLicences.getOrDefault(licensingAuthority, Collections.emptyMap());

    return newLicences.stream()
        .filter(newVehicle -> presentInDatabase(newVehicle, currentLicencesByUniqueAttributes))
        .filter(newVehicle -> attributesChanged(newVehicle, currentLicencesByUniqueAttributes))
        .map(newVehicle -> setDatabaseRelatedAttributesForUpdate(newVehicle,
            currentLicencesByUniqueAttributes, context.getUploaderId()))
        .collect(Collectors.toSet());
  }

  /**
   * Checks if {@code newLicence} is present in the database.
   *
   * @param newLicence A licence whose db presence will be checked.
   * @param currentLicencesByUniqueAttributes Licences present in the database.
   * @return true if {@code newLicence} is present in the database, false otherwise.
   */
  private boolean presentInDatabase(TaxiPhvVehicleLicence newLicence,
      Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes) {
    return currentLicencesByUniqueAttributes.containsKey(UniqueLicenceAttributes.from(newLicence));
  }

  /**
   * Checks if {@code newLicence} is absent in the database.
   *
   * @param newLicence A licence whose db presence will be checked.
   * @param currentLicencesByUniqueAttributes Licences present in the database.
   * @return false if {@code newLicence} is present in the database, true otherwise.
   */
  private boolean absentInDatabase(TaxiPhvVehicleLicence newLicence,
      Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes) {
    return !presentInDatabase(newLicence, currentLicencesByUniqueAttributes);
  }

  /**
   * Checks if attributes of {@code newLicence}, which is present in the database, have changed.
   *
   * @param newLicence A licence which is present in the database and is present in the
   *     registration submission as well.
   * @param currentLicencesByUniqueAttributes Licences present in the database.
   * @return true if {@code newLicence} has different attributes than the current one in the
   *     database.
   */
  private boolean attributesChanged(TaxiPhvVehicleLicence newLicence,
      Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes) {
    TaxiPhvVehicleLicence currentLicence = currentLicencesByUniqueAttributes
        .get(UniqueLicenceAttributes.from(newLicence));

    String newVehicleTypeDescription = newLicence.getDescription();

    String currentVehicleTypeDescription = currentLicence.getDescription();
    if (!newVehicleTypeDescription.equals(currentVehicleTypeDescription)) {
      logValueChanged("Vehicle type", currentLicence,
          currentVehicleTypeDescription, newVehicleTypeDescription);
      return true;
    }

    Boolean newWheelchairAccessible = newLicence.getWheelchairAccessible();
    Boolean currentWheelchairAccessible = currentLicence.getWheelchairAccessible();
    if (hasWheelchairAccessibleFlagChanged(newWheelchairAccessible, currentWheelchairAccessible)) {
      logValueChanged("Wheelchair accessible", currentLicence, currentWheelchairAccessible,
          newWheelchairAccessible);
      return true;
    }
    return false;
  }

  /**
   * Checks if {@code newWheelchairAccessible} has a different value than {@code
   * currentWheelchairAccessible}.
   *
   * @return true if @code newWheelchairAccessible} has a different value than {@code
   *     currentWheelchairAccessible}, false otherwise.
   */
  private boolean hasWheelchairAccessibleFlagChanged(Boolean newWheelchairAccessible,
      Boolean currentWheelchairAccessible) {
    return !Objects.equals(newWheelchairAccessible, currentWheelchairAccessible);
  }

  /**
   * Sets necessary attributes for a licence which is to be updated.
   *
   * @param newLicence A licence which is to be updated.
   * @param currentLicencesByUniqueAttributes Licences present in the database.
   * @param uploaderId ID of the uploader.
   * @return {@code newLicence} updated with its database ID and uploader-id.
   */
  private TaxiPhvVehicleLicence setDatabaseRelatedAttributesForUpdate(
      TaxiPhvVehicleLicence newLicence,
      Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes,
      UUID uploaderId) {

    TaxiPhvVehicleLicence matchingLicenceInDatabase = currentLicencesByUniqueAttributes
        .get(UniqueLicenceAttributes.from(newLicence));

    return newLicence.toBuilder()
        .id(matchingLicenceInDatabase.getId())
        .uploaderId(uploaderId)
        .build();
  }

  //------ UPDATE helper methods : end

  //------ INSERT helper methods : begin

  /**
   * Determines which licences of a given licensing authority should be inserted.
   */
  private Set<TaxiPhvVehicleLicence> computeToBeInserted(Set<TaxiPhvVehicleLicence> newLicences,
      LicensingAuthority licensingAuthority, RegisterContext context) {

    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesByUniqueAttributes = context
        .getCurrentLicencesByLicensingAuthority()
        .getOrDefault(licensingAuthority, Collections.emptyMap());

    return newLicences.stream()
        .filter(newVehicle -> absentInDatabase(newVehicle, currentLicencesByUniqueAttributes))
        .map(newVehicle -> setDatabaseRelatedAttributesForInsert(newVehicle, context))
        .collect(Collectors.toSet());
  }

  /**
   * Sets necessary attributes for a licence which is to be inserted.
   *
   * @param newLicence A licence which is to be updated.
   * @param context Context of the operation (holder of helper attributes).
   * @return {@code newLicence} updated with its uploader-id and licensing authority.
   */
  private TaxiPhvVehicleLicence setDatabaseRelatedAttributesForInsert(
      TaxiPhvVehicleLicence newLicence, RegisterContext context) {
    LicensingAuthority licensingAuthority = context.getCurrentLicensingAuthoritiesByName()
        .get(newLicence.getLicensingAuthority().getName());

    return newLicence.toBuilder()
        .uploaderId(context.getUploaderId())
        .licensingAuthority(licensingAuthority)
        .build();
  }

  //------ INSERT helper methods : end

  /**
   * Logs the information about the changed value of the {@code attributeName} attribute from {@code
   * currentValue} to {@code newValue} in {@code currentLicence}.
   */
  private <T> void logValueChanged(String attributeName, TaxiPhvVehicleLicence currentLicence,
      T currentValue, T newValue) {
    log.trace(
        "{} changed for vehicle (id={}), current value: '{}', new value: '{}'",
        attributeName,
        currentLicence.getId(),
        currentValue,
        newValue
    );
  }

  /**
   * Helper method that extracts Set of IDs from Set of {@link TaxiPhvVehicleLicence}.
   */
  private Set<Integer> toSetOfIds(Set<TaxiPhvVehicleLicence> toBeDeleted) {
    return toBeDeleted.stream().map(TaxiPhvVehicleLicence::getId).collect(Collectors.toSet());
  }

  /**
   * Helper method that extracts Set of VRMs from Set of {@link TaxiPhvVehicleLicence}.
   */
  private Set<String> toSetOfVrms(Set<TaxiPhvVehicleLicence> toBeDeleted) {
    return toBeDeleted.stream().map(TaxiPhvVehicleLicence::getVrm).collect(Collectors.toSet());
  }

  /**
   * An immutable value object holding registration-related auxiliary attributes.
   */
  @Value
  static class RegisterContext {

    /**
     * ID of the uploader.
     */
    UUID uploaderId;

    /**
     * Licences which are to be registered grouped by licensing authority.
     */
    Map<LicensingAuthority, Set<TaxiPhvVehicleLicence>> newLicencesByLicensingAuthority;

    /**
     * Licences which are present in the database grouped by licensing authority and unique
     * attributes.
     */
    Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>>
        currentLicencesByLicensingAuthority;

    /**
     * Licensing authorities present in the database grouped by name.
     */
    Map<String, LicensingAuthority> currentLicensingAuthoritiesByName;
  }

}
