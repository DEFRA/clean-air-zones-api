package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with N1 typeApproval.
 *
 * @author informed
 */
public class N1VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");

    if (vehicle.getRevenueWeight() <= 3500) {

      testNotNull(checkMassInService, vehicle, "massInService");

      if (vehicle.getMassInService() <= 1280) {
        vehicle.setVehicleType(VehicleType.SMALL_VAN);
      } else {
        vehicle.setVehicleType(VehicleType.LARGE_VAN);
      }
    } else {
      vehicle.setVehicleType(VehicleType.LARGE_VAN);
    }
  }

}
