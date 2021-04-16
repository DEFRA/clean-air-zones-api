package uk.gov.caz.taxiregister.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Helper class to transport all histories and total count.
 */
@Value
@Builder
public class LicenceInfoHistorical {

  /**
   * The total number of history changes associated with this vehicle.
   */
  long totalChangesCount;

  /**
   * A list of history changes associated with this vehicle.
   */
  List<TaxiPhvVehicleLicenceHistory> changes;
}