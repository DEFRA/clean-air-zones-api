package uk.gov.caz.vcc.domain;

import java.util.ArrayList;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * Strategy pattern implementation for assessing chargeability of a given vehicle type
 * in a class B clean air zone.
 *
 */
public class CazFrameworkClassBStrategy extends CazFrameworkClassAStrategy {

  /***
   * <p>Mark a vehicle as chargeable based on vehicle type.</p>
   * @param vehicle A vehicle.
   * @param result A charge calculation result.
   * @return The charge calculation object.
   */
  @Override
  public CalculationResult execute(Vehicle vehicle, CalculationResult result) {
    ArrayList<VehicleType> chargeableTypes = new ArrayList<>();

    chargeableTypes.add(VehicleType.HGV);

    if (chargeableTypes.contains(vehicle.getVehicleType())) {
      result.setChargeable(true);
    } else {
      result = super.execute(vehicle, result);
    }

    return result;
  }

}
