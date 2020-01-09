package uk.gov.caz.vcc.domain.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.CazClass;
import uk.gov.caz.vcc.domain.CazFrameworkClassAStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassBStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassCStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassDStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassStrategy;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;

@Component
@Slf4j
public class ChargeabilityService {

  private final String leedsCazIdentifier;
  
  /**
   * Default public constructor for ChargeabilityService.
   * 
   * @param leedsCazIdentifier UUID corresponding to the Leeds CAZ.
   */
  public ChargeabilityService(
      @Value("${application.leeds-caz-identifier}") String leedsCazIdentifier) {
    this.leedsCazIdentifier = leedsCazIdentifier;
  }

  /**
   * Public method to get the applicable charge for a given vehicle.
   *  
   * @param vehicle Vehicle whose charge is to be determined.
   * @param tariffDetails Details of the CAZ tariff to be checked.
   * @return 0 if not chargeable, else some positive float.
   */
  public float getCharge(Vehicle vehicle, TariffDetails tariffDetails) {
    if (!chargeableAgainstTariff(vehicle, tariffDetails)) {
      return (float) 0;
    }
    
    if (tariffDetails.getCazId().toString().equals(leedsCazIdentifier)) {
      return checkLeedsCazChargeability(vehicle, tariffDetails);
    }

    return checkGeneralCazChargeability(vehicle, tariffDetails);
  }
  
  /**
   * Checks if a vehicle type is chargeable in a given CAZ,
   * given its CAZ class.
   *
   * @param vehicle Vehicle whose chargeability is to be checked.
   * @param tariff Tariff against which the check is to be made.
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

    return result.getChargeable();
  }

  /**
   * Method to check vehicle chargeability against Leeds-specific rules.
   * Will default to general rules unless deemed not chargeable.
   * 
   * @param vehicle Vehicle whose charge is to be determined.
   * @param tariffDetails Details of the CAZ tariff to be checked.
   * @return 0 if not chargeable, else some positive float.
   */
  private float checkLeedsCazChargeability(Vehicle vehicle, TariffDetails tariffDetails) {
    // N.B this rule takes precedence over the existing rule in the
    // ComplianceService regarding Leeds taxis.
    if (vehicle.getIsTaxiOrPhv() && (vehicle.getIsWav() != null) && vehicle.getIsWav()) {
      return 0;
    }
    
    return checkGeneralCazChargeability(vehicle, tariffDetails);
  }

  /**
   * Method to check vehicle chargeability against a CAZ tariff.
   *
   * @param vehicle Vehicle whose charge is to be determined.
   * @param tariffDetails Details of the CAZ tariff to be checked.
   * @return 0 if not chargeable, else some positive float.
   */
  private float checkGeneralCazChargeability(Vehicle vehicle, TariffDetails tariffDetails) {

    // TODO: Add this logic back in when Disabled TaxClass changes are to be deployed.

    //    if (!tariffDetails.isDisabledTaxClassChargeable() && (isTaxClassDisabled(vehicle))) {
    //      return 0;
    //    } else {
    //      return tariffDetails.getRates()
    //          .stream()
    //          .filter(c -> c.getVehicleType().equals(vehicle.getVehicleType()))
    //          .findFirst()
    //          .map(VehicleTypeCharge::getCharge)
    //          .orElse((float) 0);
    //    }
    
    return tariffDetails.getRates()
        .stream()
        .filter(c -> c.getVehicleType().equals(vehicle.getVehicleType()))
        .findFirst()
        .map(VehicleTypeCharge::getCharge)
        .orElse((float) 0);
  }

  /**
   * Checks if a vehicle's tax class equals 'DISABLED'.
   * 
   * @param vehicle Vehicle whose tax class is to be checked.
   * @return true if equals 'DISABLED', false if null or not equal to 'DISABLED'.
   */
  private boolean isTaxClassDisabled(Vehicle vehicle) {
    if (vehicle.getTaxClass() == null) {
      log.warn("Null pointer ecnountered whilst checking for 'DISABLED' tax class for vrn: {}."
          + "Skipping this check.", vehicle.getRegistrationNumber());
      return false;
    }

    return vehicle.getTaxClass().equalsIgnoreCase("DISABLED");
  }

}
