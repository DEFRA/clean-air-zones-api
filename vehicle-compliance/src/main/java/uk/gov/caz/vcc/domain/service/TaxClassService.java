package uk.gov.caz.vcc.domain.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Domain service for working with vehicle tax classes.
 *
 */
@Service
public class TaxClassService {

  private List<String> exemptTaxClasses;

  public TaxClassService(
    @Value("${application.exempt-tax-classes:electric motorcycle,electric,disabled passenger vehicle,historic vehicle,gas}")
    String[] exemptedTaxClasses) {
    this.exemptTaxClasses = Arrays.asList(exemptedTaxClasses);
  }
  
  /**
   * Helper method to check if a vehicle's tax class is deemed exempt from
   * charging.
   * 
   * @param taxClass the tax class string literal of the vehicle.
   * @return boolean indicator for whether the tax class is deemed exempt.
   */
  public boolean isExemptTaxClass(String taxClass) {
    if (taxClass == null || taxClass.isEmpty()) {
      return false;
    }
    
    return this.exemptTaxClasses.stream().anyMatch(taxClass::equalsIgnoreCase);
  }
}
