package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with N3 typeApproval.
 * 
 * @author informed
 */
public class N3VehicleIdentifier extends VehicleIdentifier {

  @Override
  public void identifyVehicle(Vehicle vehicle) {
    vehicle.setVehicleType(VehicleType.HGV);

  }

}
