package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import java.util.ArrayList;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with L* typeApproval.
 * 
 * @author informed
 */
public class LVehicleIdentifier extends VehicleIdentifier {

  private ArrayList<String> motorcycleTypeApprovals = new ArrayList<>(7);

  /**
   * Default public constructor for VehicleIdentifier. Populates the necessary
   * collections for checking vehicle type.
   */
  public LVehicleIdentifier() {
    motorcycleTypeApprovals.add("L1");
    motorcycleTypeApprovals.add("L2");
    motorcycleTypeApprovals.add("L3");
    motorcycleTypeApprovals.add("L4");
    motorcycleTypeApprovals.add("L5");
    motorcycleTypeApprovals.add("L6");
    motorcycleTypeApprovals.add("L7");
  }
  
  /**
   * Method to identify L type approval vehicles.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {

    if (motorcycleTypeApprovals.contains(vehicle.getTypeApproval())) {
      vehicle.setVehicleType(VehicleType.MOTORCYCLE);
    } else {
      vehicle.setTypeApproval(null);
      throw new UnidentifiableVehicleException("typeApproval not recognised.");
    }
  }

}
