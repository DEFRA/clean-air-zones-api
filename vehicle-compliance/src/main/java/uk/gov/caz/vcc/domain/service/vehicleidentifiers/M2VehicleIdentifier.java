package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with M2 typeApproval.
 * 
 * @author informed
 */
public class M2VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");
    testNotNull(checkSeatingCapacity, vehicle, "seatingCapacity");

    if (vehicle.getRevenueWeight() <= 5000) {
      
      if (vehicle.getSeatingCapacity() >= 10) {
        vehicle.setVehicleType(VehicleType.MINIBUS);
      } else {
        throw new UnidentifiableVehicleException(
            "Cannot identify M2 vehicle with seating capacity < 10");
      }

    } else {
      throw new UnidentifiableVehicleException(
          "Cannot identify M2 vehicle with revenue weight > 5000kg.");
    }
  }

}
