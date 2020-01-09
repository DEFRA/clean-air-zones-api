package uk.gov.caz.vcc.domain;

import java.util.ArrayList;

public class CazFrameworkClassDStrategy extends CazFrameworkClassCStrategy {

  /***
   * <p>Mark a vehicle as chargeable based on vehicle type.</p>
   * @param vehicle A vehicle.
   * @param result A charge calculation result.
   * @return The charge calculation object.
   */
  @Override
  public CalculationResult execute(Vehicle vehicle, CalculationResult result) {
    ArrayList<VehicleType> chargeableTypes = new ArrayList<>();

    chargeableTypes.add(VehicleType.PRIVATE_CAR);
    chargeableTypes.add(VehicleType.MOTORCYCLE);

    if (chargeableTypes.contains(vehicle.getVehicleType())) {
      result.setChargeable(true);
    } else {
      result = super.execute(vehicle, result);
    }

    return result;
  }

}
