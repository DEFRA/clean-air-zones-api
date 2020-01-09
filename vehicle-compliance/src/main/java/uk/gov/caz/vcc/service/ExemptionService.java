package uk.gov.caz.vcc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.TaxClassService;
import uk.gov.caz.vcc.domain.service.TypeApprovalService;

/**
 * Application Service to determine if a vehicle is exempt and update the
 * calculationResult accordingly.
 */
@Service
@RequiredArgsConstructor
public class ExemptionService {

  private final MilitaryVehicleService militaryVehicleService;

  private final RetrofitService retrofitService;

  private final FuelTypeService fuelTypeService;

  private final TaxClassService taxClassService;

  private final TypeApprovalService typeApprovalService;

  /**
   * Public method to check if a given vehicle is exempt and update the
   * calculationResult accordingly.
   * 
   * @param vehicle           Vehicle whose exemption is to be determined.
   * @param calculationResult CalculationResult for the vehicle in question.
   * @return
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle,
      CalculationResult calculationResult) {

    String registrationNumber = vehicle.getRegistrationNumber();
    if (fuelTypeService.isExemptFuelType(vehicle.getFuelType())
        || taxClassService.isExemptTaxClass(vehicle.getTaxClass())
        || typeApprovalService.isExemptTypeApproval(vehicle.getTypeApproval())
        || militaryVehicleService.isMilitaryVehicle(registrationNumber)
        || retrofitService.isRetrofitted(registrationNumber)) {
      calculationResult.setExempt(true);
      return calculationResult;
    }
    return calculationResult;
  }
}
