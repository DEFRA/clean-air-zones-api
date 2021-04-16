package uk.gov.caz.vcc.dto;

import com.google.common.base.Preconditions;
import java.util.Map;
import lombok.Value;

/**
 * Helper class that stores results from bulk-check DVLA search call.
 */
@Value
public class DvlaVehiclesInformation {

  Map<String, SingleDvlaVehicleData> byVrn;

  /**
   * Creates successful {@link DvlaVehiclesInformation} instance.
   *
   * @param dvlaVehiclesInformation Response map from VRN to {@link SingleDvlaVehicleData}
   *     instances. Remarks: each {@link SingleDvlaVehicleData} represents separate network call and
   *     may be success or failure.
   */
  public static DvlaVehiclesInformation success(
      Map<String, SingleDvlaVehicleData> dvlaVehiclesInformation) {
    Preconditions.checkNotNull(dvlaVehiclesInformation, "'dvlaVehiclesInformation' cannot be null");
    return new DvlaVehiclesInformation(dvlaVehiclesInformation);
  }

  /**
   * Creates failed {@link DvlaVehiclesInformation} instance. Such instance means that there was a
   * serious error during bulk calls to DVLA and there are no data available at all.
   */
  public static DvlaVehiclesInformation failure() {
    return new DvlaVehiclesInformation(null);
  }

  /**
   * Returns true if {@link DvlaVehiclesInformation} instance is failed or false if successful.
   */
  public boolean hasFailed() {
    return byVrn == null;
  }

  /**
   * Gets {@link SingleDvlaVehicleData} for VRN. Can be null if whole {@link
   * DvlaVehiclesInformation} is in failed stated.
   */
  public SingleDvlaVehicleData getDvlaVehicleInfoFor(String vrn) {
    return hasFailed() ? null : byVrn.get(vrn);
  }
}
