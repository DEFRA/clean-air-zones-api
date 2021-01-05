package uk.gov.caz.accounts.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;

/**
 * A helper object that holds data required for a response for {@code GET
 * /v1/accounts/{accountId}/vehicles} endpoint.
 */
@Value
@Builder
public class VehiclesWithAnyUndeterminedChargeabilityFlagData {

  Page<AccountVehicle> vehicles;
  boolean anyUndeterminedVehicles;

  public boolean containsAnyUndeterminedVehicles() {
    return anyUndeterminedVehicles;
  }
}
