package uk.gov.caz.vcc.domain;

import uk.gov.caz.definitions.domain.Vehicle;

/**
 * Interface to be implemented by classes checking for each CAZ class (A,B, C, D).
 * 
 */
public interface CazFrameworkClassStrategy {
  
  public CalculationResult execute(Vehicle vehicle, CalculationResult result);

}
