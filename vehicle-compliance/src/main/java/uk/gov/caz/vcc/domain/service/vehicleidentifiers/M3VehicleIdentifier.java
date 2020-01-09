package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with M3 typeApproval.
 * 
 * @author informed
 */
public class M3VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");

    if (vehicle.getRevenueWeight() >= 5000) {
      vehicle.setVehicleType(VehicleType.BUS);
    } else {
      vehicle.setVehicleType(VehicleType.MINIBUS);
    }
  }

}
