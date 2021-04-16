package uk.gov.caz.vcc.domain;

import java.util.ArrayList;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * Strategy pattern implementation for assessing chargeability of a given vehicle type
 * in a class A clean air zone.
 *
 */
public class CazFrameworkClassAStrategy implements CazFrameworkClassStrategy {

  /***
   * <p>Mark a vehicle as chargeable based on vehicle type.</p>
   * @param vehicle A vehicle.
   * @param result A charge calculation result.
   * @return The charge calculation object.
   */
  public CalculationResult execute(Vehicle vehicle, CalculationResult result) {
    ArrayList<VehicleType> chargeableTypes = new ArrayList<>();

    chargeableTypes.add(VehicleType.BUS);
    chargeableTypes.add(VehicleType.COACH);
    chargeableTypes.add(VehicleType.TAXI_OR_PHV);
    
    boolean isChargeable = chargeableTypes.contains(vehicle.getVehicleType());

    result.setChargeable(isChargeable);

    return result;
  }

}
