package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import org.springframework.stereotype.Service;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * VehicleIdentifer class for vehicles with invalid of no typeApproval and a motorhome body type.
 * 
 * @author informed
 */
@Service
public class MotorhomeVehicleIdentifier extends VehicleIdentifier {
  
  /**
   * Method to identify vehicles with an invalid type approval and the motorhome body type.
   */
  @Override
  public void identifyVehicle(Vehicle vehicle) {
    checkVehicleWeight(vehicle);
  }
  
  /**
   * Method to check the weight of motorhome and 
   * either identify the vehicle as a car or perform a seating capacity check.
   * @param vehicle Vehicle whose vehicle type is to be determined.
   */
  private void checkVehicleWeight(Vehicle vehicle) {
    if (vehicle.getRevenueWeight() == null || vehicle.getRevenueWeight() <= 3500) {
      vehicle.setVehicleType(VehicleType.VAN);
    } else {
      checkSeatingCapacity(vehicle);
    }
  }
  
  /**
   * Method to check the seating capacity of a motorhome and identify it accordingly.
   * @param vehicle Vehicle whose vehicle type is to be determined.
   */
  private void checkSeatingCapacity(Vehicle vehicle) {
    if (vehicle.getSeatingCapacity() != null && vehicle.getSeatingCapacity() > 8) {
      if (vehicle.getRevenueWeight() <= 5000) {
        vehicle.setVehicleType(VehicleType.MINIBUS); 
      } else {
        vehicle.setVehicleType(VehicleType.BUS);
      }
    } else {
      vehicle.setVehicleType(VehicleType.HGV);
    }
  }
}