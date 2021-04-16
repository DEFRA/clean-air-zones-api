package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import java.util.function.Predicate;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;

/**
 * Abstract class for any class which performs an identification for a vehicle
 * with a given typeApproval. Is a strategy pattern implementation, with one
 * concrete strategy per typeApproval. The naming convention for classes
 * implementing this interface is _typeApproval_VehicleIdentifier, where
 * _typeApproval_ is the typeApproval handled by that VehicleIdentifier.
 * 
 */
public abstract class VehicleIdentifier {

  protected final Predicate<Vehicle> checkRevenueWeight;
  protected final Predicate<Vehicle> checkMassInService;
  protected final Predicate<Vehicle> checkSeatingCapacity;
  protected final Predicate<Vehicle> checkTaxClass;
  protected final Predicate<Vehicle> checkBodyType;

  /**
   * Sets the vehicleType on the Vehicle provided. If the vehicleType cannot be
   * determined, an UnidentifiableVehicleException is thrown.
   * 
   * @param vehicle Vehicle whose vehicleType is to be determined.
   */
  public abstract void identifyVehicle(Vehicle vehicle);

  /**
   * Default public constructor for a VehicleIdentifier. Instantiates the
   * various Predicates required throughout the flow for protecting against
   * NullPointerErrors.
   */
  public VehicleIdentifier() {
    checkRevenueWeight = v -> v.getRevenueWeight() == null || v.getRevenueWeight().equals(0);
    checkMassInService = v -> v.getMassInService() == null || v.getMassInService().equals(0);
    checkSeatingCapacity = v -> v.getSeatingCapacity() == null || v.getSeatingCapacity().equals(0);
    checkTaxClass = v -> v.getTaxClass() == null || v.getTaxClass().equals("");
    checkBodyType = v -> v.getBodyType() == null || v.getBodyType().equals("");
  }

  /**
   * Raised an UnidentifiableVehicleException if the passed Predicate evaluates
   * to true.
   * 
   * @param p         Predicate to be checked.
   * @param vehicle   Vehicle against which the predicate is to be applied.
   * @param attribute String value of the attribute being checked.
   */
  protected void testNotNull(Predicate<Vehicle> p, Vehicle vehicle,
      String attribute) {
    if (p.test(vehicle)) {
      throw new UnidentifiableVehicleException(
          "Cannot identify vehicle with null: " + attribute);
    }
  }
}
