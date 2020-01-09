package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with N2 typeApproval.
 * 
 * @author informed
 */
public class N2VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkRevenueWeight, vehicle, "revenueWeight");

    if (vehicle.getRevenueWeight() <= 3500) {
      throw new UnidentifiableVehicleException(
          "Cannot identify N2 vehicle with revenue weight <= 3500kg.");
    } else {
      vehicle.setVehicleType(VehicleType.HGV);
    }
  }

}
