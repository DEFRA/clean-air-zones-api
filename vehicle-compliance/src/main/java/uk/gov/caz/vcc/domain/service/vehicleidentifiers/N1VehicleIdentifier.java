package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with N1 typeApproval.
 *
 * @author informed
 */
public class N1VehicleIdentifier extends VehicleIdentifier {

  /**
   * Method to identify N1 type approval vehicles.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");

    if (vehicle.getRevenueWeight() <= 3500) {
      vehicle.setVehicleType(VehicleType.VAN);
    } else {
      throw new UnidentifiableVehicleException(
          "Cannot identify N1 vehicle with revenue weight > 3500kg.");
    }
  }

}
