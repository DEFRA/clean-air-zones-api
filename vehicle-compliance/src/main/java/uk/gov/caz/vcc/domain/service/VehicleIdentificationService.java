package uk.gov.caz.vcc.domain.service;

import org.springframework.stereotype.Component;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.LVehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M1VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M2VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M3VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N1VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N2VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N3VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.NullVehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.TVehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.VehicleIdentifier;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

/**
 * Domain service for identifying a vehicle's type against the CAZ framework
 * based on externally-sourced data attributes.
 */
@Component
public class VehicleIdentificationService {

  private final UnidentifiableVehicleExceptionHandler unidentifiableVehicleExceptionHandler;
  private final TVehicleIdentifier agriculturalVehicleIdentifier;

  /**
   * Default constructor for domain service to identifying a vehicles type against
   * the CAZ framework based on externally-sourced data attributes.
   */
  public VehicleIdentificationService(UnidentifiableVehicleExceptionHandler 
      unidentifiableVehicleExceptionHandler,
      TVehicleIdentifier agriculturalVehicleIdentifier) {
    this.unidentifiableVehicleExceptionHandler = unidentifiableVehicleExceptionHandler;
    this.agriculturalVehicleIdentifier = agriculturalVehicleIdentifier;
  }

  /**
   * Sets the vehicle type of a Vehicle, given a series of attributes.
   * 
   * @param vehicle Vehicle whose type is to be determined.
   */
  public void setVehicleType(Vehicle vehicle) {

    VehicleIdentifier identifier;

    // Calls the appropriate VehicleIdentifier for a Vehicle, given its
    // typeApproval.
    try {
      if (vehicle.getTypeApproval() == null) {
        identifier = new NullVehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("M1")) {
        identifier = new M1VehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("M2")) {
        identifier = new M2VehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("M3")) {
        identifier = new M3VehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("N1")) {
        identifier = new N1VehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("N2")) {
        identifier = new N2VehicleIdentifier();
      } else if (vehicle.getTypeApproval().equals("N3")) {
        identifier = new N3VehicleIdentifier();
      } else if (vehicle.getTypeApproval().startsWith("T")) {
        identifier = agriculturalVehicleIdentifier;
      } else if (vehicle.getTypeApproval().startsWith("L")) {
        identifier = new LVehicleIdentifier();
      } else {
        // If not a recognised type approval, set value to null and
        // use null identifier logic.
        vehicle.setTypeApproval(null);
        identifier = new NullVehicleIdentifier();
      }

      identifier.identifyVehicle(vehicle);

    } catch (UnidentifiableVehicleException notIdentifiedEx) {
      // if the vehicle is unidentified attempt to identify it 
      // using tax class and body type
      try {
        identifier = new NullVehicleIdentifier();
        identifier.identifyVehicle(vehicle);

      } catch (UnidentifiableVehicleException e) {
        unidentifiableVehicleExceptionHandler.handleError(e, vehicle);
        vehicle.setVehicleType(null);
      }
    }
  }

}
