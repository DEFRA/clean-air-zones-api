package uk.gov.caz.vcc.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.domain.VehicleType;

@Builder
@Value
public class VehicleEntrantReportingRequest {
  
  /**
   * Unique identifer.
   */
  String correlationId;
  
  /**
   * Unique identifier for the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * String containing the unique Vehicle Registration Number of the witnessed vehicle without
   * whitespace and SHA256 hashed.
   */
  String vrnHash;
  
  /**
   * The datetime that the vehicle entered the Clean Air Zone.
   */
  String hour;

  /**
   * Vehicle type approval category.
   */
  String typeApproval;

  /**
   * Fuel type category.
   */
  String fuelType;

  /**
   * Charge validity code.
   */
  String chargeValidityCode;

  /**
   * Vehicle type in accordance with CAZ framework.
   */
  VehicleType vehicleType;

  /**
   * Vehicle make.
   */
  String make;

  /**
   * Vehicle model.
   */
  String model;

  /**
   * Vehicle colour.
   */
  String colour;

  /**
   * The reason for which the vehicle is exempted (if applicable).
   */
  String exemptionReason;

  /**
   * Description returned from taxi register (if applicable).
   */
  String taxiPhvDescription;

  /**
   * List of Licensing Authority that the taxi or Private Hire vehicle is licensed with.
   */
  List<String> licensingAuthorities;
  
  /**
   * Boolean indicating if vehicle is non UK.
   */
  boolean nonStandardUkPlateFormat;
  
}
