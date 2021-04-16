package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with N3 typeApproval.
 * 
 * @author informed
 */
public class N3VehicleIdentifier extends VehicleIdentifier {

  /**
   * Method to identify N3 type approval vehicles as HGVs.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {
    vehicle.setVehicleType(VehicleType.HGV);

  }

}
