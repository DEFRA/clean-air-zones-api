package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with M2 typeApproval.
 * 
 * @author informed
 */
public class M2VehicleIdentifier extends VehicleIdentifier {

  /**
   * Method to identify M2 type approval vehicles.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");
    testNotNull(checkSeatingCapacity, vehicle, "seatingCapacity");

    if (vehicle.getRevenueWeight() <= 5000) {
      
      if (vehicle.getSeatingCapacity() >= 9) {
        vehicle.setVehicleType(VehicleType.MINIBUS);
      } else {
        throw new UnidentifiableVehicleException(
            "Cannot identify M2 vehicle with seating capacity < 9");
      }

    } else {
      throw new UnidentifiableVehicleException(
          "Cannot identify M2 vehicle with revenue weight > 5000kg.");
    }
  }

}
