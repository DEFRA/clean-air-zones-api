package uk.gov.caz.vcc.domain;

/**
 * Interface to be implemented by classes checking for each CAZ class (A,B, C, D).
 * 
 * @author informed
 */
public interface CazFrameworkClassStrategy {
  
  public CalculationResult execute(Vehicle vehicle, CalculationResult result);

}
