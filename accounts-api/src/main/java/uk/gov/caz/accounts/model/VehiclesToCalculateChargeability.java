package uk.gov.caz.accounts.model;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Wraps set of {@link AccountVehicleBare} instances and provides convenient helper methods to work
 * over this set.
 */
@AllArgsConstructor
@Value
public class VehiclesToCalculateChargeability {

  /**
   * Low level set of Account Vehicle IDs combined with VRNs for which charge calculation should be
   * calculated.
   */
  @Getter(AccessLevel.NONE)
  Set<AccountVehicleBare> vehiclesToCalculate;

  /**
   * Returns new instance of {@link VehiclesToCalculateChargeability} class instantiated with set
   * with only single vehicle.
   *
   * @param accountVehicleId Account Vehicle ID.
   * @param vrn VRN of the vehicles.
   * @return new instance of {@link VehiclesToCalculateChargeability} class instantiated with set
   *     with only single vehicle.
   */
  public static VehiclesToCalculateChargeability fromSingle(UUID accountVehicleId, String vrn) {
    return new VehiclesToCalculateChargeability(
        newHashSet(new AccountVehicleBare(accountVehicleId, vrn)));
  }

  /**
   * Creates subset limited to {@code limit} number of vehicles.
   *
   * @param limit Determines limit of the subset.
   * @return New instance of {@link VehiclesToCalculateChargeability} with limited vehicles.
   */
  public VehiclesToCalculateChargeability subsetLimitedTo(int limit) {
    return new VehiclesToCalculateChargeability(
        ImmutableSet.copyOf(Iterables.limit(vehiclesToCalculate, limit)));
  }

  /**
   * Returns {@link Set} of {@link UUID} with vehicle IDs to process.
   *
   * @return {@link Set} of {@link UUID} with vehicle IDs to process.
   */
  public Set<UUID> setOfIDs() {
    return vehiclesToCalculate.stream().map(AccountVehicleBare::getAccountVehicleId).collect(
        Collectors.toSet());
  }

  /**
   * Returns {@link Set} of {@link String} with vehicle VRNs to process.
   *
   * @return {@link Set} of {@link String} with vehicle VRNs to process.
   */
  public Set<String> setOfVrns() {
    return vehiclesToCalculate.stream().map(AccountVehicleBare::getVrn)
        .collect(Collectors.toSet());
  }

  /**
   * Partitions internal set to list of sets with max size equal to {@code batchSize}.
   */
  public List<VehiclesToCalculateChargeability> toBatchesOfSize(int batchSize) {
    Iterable<List<AccountVehicleBare>> batches = Iterables
        .partition(vehiclesToCalculate, batchSize);
    List<VehiclesToCalculateChargeability> listOfBatches = newArrayList();
    for (List<AccountVehicleBare> batch : batches) {
      listOfBatches.add(new VehiclesToCalculateChargeability(ImmutableSet.copyOf(batch)));
    }
    return listOfBatches;
  }

  /**
   * Returns number of vehicles to process.
   *
   * @return number of vehicles to process.
   */
  public int size() {
    return vehiclesToCalculate.size();
  }

  /**
   * Gets matching Account Vehicle ID for specified VRN.
   *
   * @param searchedVrn VRN to match. It will be normalized.
   * @return UUID with Account Vehicle ID for specified VRN.
   * @throws RuntimeException if there is no matching Account Vehicle ID.
   */
  public UUID getAccountVehicleIdFor(String searchedVrn) {
    String normalizedSearchedVrn = normalize(searchedVrn);
    return vehiclesToCalculate.stream()
        .filter(accountVehicleBare -> normalize(accountVehicleBare.getVrn())
            .equals(normalizedSearchedVrn))
        .findFirst().map(AccountVehicleBare::getAccountVehicleId)
        .orElseThrow(() -> new NoSuchElementException("Unable to find matching vehicle"));
  }

  /**
   * Removes trailing zeros and whitespaces, leading whitespaces, makes lowercase.
   */
  private String normalize(String vrn) {
    return CharMatcher.is('0').trimLeadingFrom(vrn.trim().toLowerCase());
  }
}
