package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with M1 typeApproval.
 * 
 * @author informed
 */
public class M1VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {
    vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
  }

}
