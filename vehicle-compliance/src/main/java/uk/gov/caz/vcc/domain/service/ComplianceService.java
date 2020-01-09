package uk.gov.caz.vcc.domain.service;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusPresentComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.LeedsComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusNullComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.UnableToIdentifyVehicleComplianceException;

/**
 * Domain service to determine whether or not a Vehicle is compliant.
 * 
 * @author informed
 */
@Slf4j
@Component
public class ComplianceService {
  private String leedsCazIdentifier;
  private LeedsComplianceService leedsComplianceService;
  private EuroStatusNullComplianceService euroStatusNullComplianceService;
  private EuroStatusPresentComplianceService euroStatusPresentComplianceService;

  /**
   * Public constructor for the ComplianceService.
   * 
   * @param romanNumeralConverter Instantiation of RomanNumeralConverter.
   */
  public ComplianceService(
          @Value("${application.leeds-caz-identifier}") String leedsCazIdentifier,
          LeedsComplianceService leedsComplianceService,
          EuroStatusNullComplianceService euroStatusNullComplianceService,
          EuroStatusPresentComplianceService euroStatusPresentComplianceService) {
    this.leedsCazIdentifier = leedsCazIdentifier;
    this.leedsComplianceService = leedsComplianceService;
    this.euroStatusNullComplianceService = euroStatusNullComplianceService;
    this.euroStatusPresentComplianceService = euroStatusPresentComplianceService;
  }
  
  /**
   * Method to determine if a Vehicle is compliant in a given CAZ.
   * 
   * @param vehicle Vehicle whose compliance is to be determined.
   * @param cazId     UUID of the CAZ for which compliance is being determined.
   * @return boolean {@code true} if compliant, else {@code false}.
   */
  private boolean isVehicleCompliant(Vehicle vehicle, UUID cazId) {
    if (cazId.toString().equals(this.leedsCazIdentifier)) {
      try {
        return this.leedsComplianceService.isVehicleCompliance(vehicle);
      } catch (UnableToIdentifyVehicleComplianceException e) {
        log.warn(e.getMessage());
      }
    }
    try{
      if (vehicle.getEuroStatus() == null) {
        return this.euroStatusNullComplianceService.isVehicleCompliance(vehicle);
      } else {
        return this.euroStatusPresentComplianceService.isVehicleCompliance(vehicle);
      }
    } catch (Exception ex) {
      throw new UnableToIdentifyVehicleComplianceException(ex.getMessage());
    }
  }

  /**
   * ArgumentOutOfRangeException Set whether the charge calculation is
   * compliant.
   * 
   * @param vehicle A vehicle.
   * @param result  A charge calculation result.
   * @param cazId UUID for the CAZ to be checked.
   * @return The charge calculation object.
   */
  public CalculationResult updateCalculationResult(Vehicle vehicle,
      CalculationResult result) {

    result.setCompliant(this.isVehicleCompliant(vehicle, result.getCazIdentifier()));

    return result;
  }
}
