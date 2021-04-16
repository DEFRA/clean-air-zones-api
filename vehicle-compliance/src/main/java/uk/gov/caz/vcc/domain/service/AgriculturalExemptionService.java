package uk.gov.caz.vcc.domain.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

/**
 * Domain service for determining agricultural vehicle exemption.
 *
 */
@Service
public class AgriculturalExemptionService { 
  
  private final List<String> exemptBodyTypes;
  private final List<String> exemptAgriculturalTaxClasses;
  private final List<String> validTypeApprovals;
  
  /**
   * Default constructor for domain service for identifying exempt agricultural vehicles.
   */
  public AgriculturalExemptionService(
      @Value("${application.exempt-body-types:"
          + "rootcropharvester, forageharvester, windrower, sprayer, viner/picker}") 
      String[] exemptBodyTypes,
      @Value("${application.exempt-agricultural-tax-classes:agriculturalmachine}") 
      String[] exemptAgriculturalTaxClasses, 
      @Value("$application.valid-type-approvals:"
          + "M1,M2,M3,N1,N2,N3,T1,T2,T3,T4,L5,L1,L2,L3,L4,L5,L6,L7")
      String [] validTypeApprovals) {
    this.exemptBodyTypes = Arrays.asList(exemptBodyTypes);
    this.exemptAgriculturalTaxClasses = Arrays.asList(exemptAgriculturalTaxClasses);
    this.validTypeApprovals = Arrays.asList(validTypeApprovals);
  }
  
  /**
   * Helper method to check if Vehicle's tax class and body type is exempt from charging.
   * 
   * @param vehicle the {@Vehicle} to check for exemption
   * @return boolean indicator to determine if vehicle is an exempt agricultural vehicle.
   */
  public boolean isExemptAgriculturalVehicle(Vehicle vehicle) {
    if (vehicle.getTypeApproval() == null 
        || !(validTypeApprovals.contains(vehicle.getTypeApproval()))) {
      if (vehicle.getTaxClass() != null && this.exemptAgriculturalTaxClasses.stream().anyMatch(
          vehicle.getTaxClass().replaceAll("\\s+", "")::equalsIgnoreCase)) {
        return true;
      } else if (vehicle.getBodyType() != null && this.exemptBodyTypes.stream().anyMatch(
          vehicle.getBodyType().replaceAll("\\s+", "")::equalsIgnoreCase)) {
        return true;
      }
    }
    return false;
  }
}
