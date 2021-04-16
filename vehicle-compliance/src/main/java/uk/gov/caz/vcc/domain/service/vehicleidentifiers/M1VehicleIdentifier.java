package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with M1 typeApproval.
 * 
 */
public class M1VehicleIdentifier extends VehicleIdentifier {

  /**
   * Method to identify M1 type approval vehicles.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {
    if (vehicle.getBodyType() != null && vehicle.getBodyType()
        .replaceAll("\\s+", "").equals("motorhome/caravan")) {
      MotorhomeVehicleIdentifier identifier = new MotorhomeVehicleIdentifier();
      identifier.identifyVehicle(vehicle);
    } else {
      vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
    }
  }

}
