package uk.gov.caz.vcc.service;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.service.AgriculturalExemptionService;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.TaxClassService;
import uk.gov.caz.vcc.domain.service.TypeApprovalService;

/**
 * Application Service to determine if a vehicle is exempt and update the calculationResult
 * accordingly.
 */
@Service
@RequiredArgsConstructor
public class ExemptionService {

  private final FuelTypeService fuelTypeService;
  private final TaxClassService taxClassService;
  private final TypeApprovalService typeApprovalService;
  private final GeneralWhitelistService generalWhitelistService;
  private final MilitaryVehicleService militaryVehicleService;
  private final AgriculturalExemptionService agriculturalExemptionService;

  /**
   * Public method to check if a given vehicle is exempt and update the calculationResult
   * accordingly.
   *
   * @param vehicle Vehicle whose exemption is to be determined.
   * @param calculationResult CalculationResult for the vehicle in question.
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle,
      CalculationResult calculationResult) {
    Exempt exempt = new Exempt(vehicle);
    Supplier<Boolean> militaryFlagSupplier = exempt::becauseIsMilitaryVehicle;
    Supplier<Boolean> whitelistFlagSupplier = exempt::becauseIsOnGeneralWhitelist;
    return updateCalculationResult(calculationResult, exempt, militaryFlagSupplier,
        whitelistFlagSupplier);
  }
  
  /**
   * Public method (overload) to check if a given vehicle is exempt and update the calculationResult
   * accordingly.
   *
   * @param vehicle Vehicle whose exemption is to be determined.
   * @param calculationResult CalculationResult for the vehicle in question.
   * @param isMilitaryVehicle a flag that can be used in the event
   *        military status is known at the point of invocation.
   * @param isExemptOnGeneralWhitelist a flag that can be used in the event
   *        exemption status due to existing on the general Whitelist is known
   *        at the point of invocation.
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle,
      CalculationResult calculationResult, boolean isMilitaryVehicle,
      boolean isExemptOnGeneralWhitelist) {
    Exempt exempt = new Exempt(vehicle);
    return updateCalculationResult(calculationResult, exempt,
        () -> isMilitaryVehicle, () -> isExemptOnGeneralWhitelist);
  }
  
  /**
   * Private helper method to update calculation results based on a supplied
   * method signature (depending on which public method has been invoked).
   */
  private CalculationResult updateCalculationResult(CalculationResult calculationResult,
      Exempt exempt, Supplier<Boolean> isMilitaryVehicle,
      Supplier<Boolean> isExemptOnGeneralWhitelist) {
    if (exempt.byFuelType()
        || exempt.byTaxClass()
        || exempt.byTypeApproval()
        || exempt.agriculturalByTaxClassOrBodyType()
        || isMilitaryVehicle.get()
        || isExemptOnGeneralWhitelist.get()) {
      calculationResult.setExempt(true);
      return calculationResult;
    }
    return calculationResult;
  }
  
  
  /**
   * Identifies the reason for an exemption.
   */
  public String identifyExemptionReason(Vehicle vehicle) {    
    Exempt exempt = new Exempt(vehicle);
    
    if (exempt.byFuelType()) {
      return vehicle.getFuelType();
    }
    
    if (exempt.byTaxClass()) {
      return vehicle.getTaxClass();
    }
    
    if (exempt.byTypeApproval()) {
      return vehicle.getVehicleType().toString();
    }
    
    if (exempt.agriculturalByTaxClassOrBodyType()) {
      return "Agricultural vehicle";
    }
    
    if (exempt.becauseIsOnGeneralWhitelist()) {
      Optional<String> category = generalWhitelistService
          .getExemptionCategory(vehicle.getRegistrationNumber());
      if (category.isPresent()) {
        return category.get();
      }
    }
    
    return "Other";
  }


  /**
   * Checks if a {@link Vehicle} is exempt by virtue of its fuel type, tax class or type approval.
   */
  public boolean isVehicleExempted(Vehicle vehicle) {
    return fuelTypeService.isExemptFuelType(vehicle.getFuelType())
        || taxClassService.isExemptTaxClass(vehicle.getTaxClass())
        || typeApprovalService.isExemptTypeApproval(vehicle.getTypeApproval())
        || agriculturalExemptionService.isExemptAgriculturalVehicle(vehicle);
  }

  /**
   * Class that helps in readability of exemptions check chain.
   */
  private class Exempt {

    private final Vehicle vehicle;

    /**
     * Initializes new instance of {@link Exempt} class.
     *
     * @param vehicle {@link Vehicle} which properties will be queried to see if it is exempt.
     */
    public Exempt(Vehicle vehicle) {
      this.vehicle = vehicle;
    }

    /**
     * Returns true if vehicle is exempt by fuel type.
     */
    public boolean byFuelType() {
      return fuelTypeService.isExemptFuelType(vehicle.getFuelType());
    }

    /**
     * Returns true if vehicle is exempt by tax class.
     */
    public boolean byTaxClass() {
      return taxClassService.isExemptTaxClass(vehicle.getTaxClass());
    }

    /**
     * Returns true if vehicle is exempt by type approval.
     */
    public boolean byTypeApproval() {
      return typeApprovalService.isExemptTypeApproval(vehicle.getTypeApproval());
    }
    
    /**
     * Returns true if agricultural vehicle is exempt by tax class or body type.
     * @return boolean to indicate if exempt.
     */
    public boolean agriculturalByTaxClassOrBodyType() {
      return agriculturalExemptionService.isExemptAgriculturalVehicle(vehicle);
    }

    /**
     * Returns true if vehicle is a military vehicle (MOD).
     */
    public boolean becauseIsMilitaryVehicle() {
      return militaryVehicleService.isMilitaryVehicle(vehicle.getRegistrationNumber());
    }

    /**
     * Returns true if vehicle is on a General Purpose Whitelist (of exempt/compliant vehicles).
     */
    public boolean becauseIsOnGeneralWhitelist() {
      return generalWhitelistService.exemptOnGeneralWhitelist(vehicle.getRegistrationNumber());
    }
  }
}
