package uk.gov.caz.whitelist.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Helper class to transport all histories and total count.
 */
@Value
@Builder
public class WhitelistVehicleHistorical {

  /**
   * The total number of history changes associated with this vehicle.
   */
  int totalChangesCount;

  /**
   * A list of history changes associated with this vehicle.
   */
  List<WhitelistVehicleHistory> changes;
}
