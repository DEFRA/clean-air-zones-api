package uk.gov.caz.accounts.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key for {@link VehicleChargeability} entity.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VehicleChargeabilityId implements Serializable {

  /**
   * Account vehicle ID.
   */
  private UUID accountVehicleId;

  /**
   * CAZ ID.
   */
  private UUID cazId;
}
