package uk.gov.caz.accounts.repository;

import java.util.Set;
import java.util.UUID;

/**
 * Custom extension for Spring-Data default {@link VehicleChargeabilityRepository}.
 */
public interface VehicleChargeabilityRepositoryCustom {

  /**
   * Delete all rows from T_VEHICLE_CHARGEABILITY with IDs in set of {@code ids}.
   *
   * @param ids Set of account vehicle IDs to delete.
   */
  void deleteFromVehicleChargeability(Set<UUID> ids);
}
