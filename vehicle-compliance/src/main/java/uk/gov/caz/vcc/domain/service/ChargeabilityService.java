package uk.gov.caz.vcc.domain.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.CazClass;
import uk.gov.caz.vcc.domain.CazFrameworkClassAStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassBStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassCStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassDStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassStrategy;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;

@Component
@Slf4j
public class ChargeabilityService {

  /**
   * Default public constructor for ChargeabilityService.
   */
  public ChargeabilityService() {
  }

  /**
   * Public method to get the applicable charge for a given vehicle.
   * 
   * @param vehicle       Vehicle whose charge is to be determined.
   * @param tariffDetails Details of the CAZ tariff to be checked.
   * @return 0 if not chargeable, else some positive float.
   */
  public float getCharge(Vehicle vehicle, TariffDetails tariffDetails) {
    if (!chargeableAgainstTariff(vehicle, tariffDetails)) {
      return (float) 0;
    }
    return checkGeneralCazChargeability(vehicle, tariffDetails);
  }

  /**
   * Checks if a vehicle type is chargeable in a given CAZ, given its CAZ class.
   *
   * @param vehicle Vehicle whose chargeability is to be checked.
   * @param tariff  Tariff against which the check is to be made.
   * @return
   */
  private boolean chargeableAgainstTariff(Vehicle vehicle, TariffDetails tariff) {

    CalculationResult result = new CalculationResult();
    CazFrameworkClassStrategy strategy;

    if (tariff.getCazClass() == CazClass.A) {
      strategy = new CazFrameworkClassAStrategy();
    } else if (tariff.getCazClass() == CazClass.B) {
      strategy = new CazFrameworkClassBStrategy();
    } else if (tariff.getCazClass() == CazClass.C) {
      strategy = new CazFrameworkClassCStrategy();
    } else {
      strategy = new CazFrameworkClassDStrategy();
    }

    result = strategy.execute(vehicle, result);
    return result.isChargeable();
  }

  /**
   * Method to check vehicle chargeability against a CAZ tariff.
   *
   * @param vehicle       Vehicle whose charge is to be determined.
   * @param tariffDetails Details of the CAZ tariff to be checked.
   * @return 0 if not chargeable, else some positive float.
   */
  private float checkGeneralCazChargeability(Vehicle vehicle, TariffDetails tariffDetails) {
    return tariffDetails.getRates().stream()
        .filter(c -> c.getVehicleType().equals(vehicle.getVehicleType())).findFirst()
        .map(VehicleTypeCharge::getCharge).orElse((float) 0);
  }
}
