package uk.gov.caz.vcc.domain;

import java.util.ArrayList;

public class CazFrameworkClassCStrategy extends CazFrameworkClassBStrategy {

  /***
   * <p>Mark a vehicle as chargeable based on vehicle type.</p>
   * @param vehicle A vehicle.
   * @param result A charge calculation result.
   * @return The charge calculation object.
   */
  @Override
  public CalculationResult execute(Vehicle vehicle, CalculationResult result) {
    ArrayList<VehicleType> chargeableTypes = new ArrayList<>();

    chargeableTypes.add(VehicleType.LARGE_VAN);
    chargeableTypes.add(VehicleType.SMALL_VAN);
    chargeableTypes.add(VehicleType.MINIBUS);

    if (chargeableTypes.contains(vehicle.getVehicleType())) {
      result.setChargeable(true);
    } else {
      result = super.execute(vehicle, result);
    }

    return result;
  }

}
