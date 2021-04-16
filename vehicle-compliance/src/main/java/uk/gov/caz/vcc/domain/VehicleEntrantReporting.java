package uk.gov.caz.vcc.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model that represents single report of an entrant.
 */
@Entity
@Getter
@Table(name = "t_vehicle_entrant_reporting", schema = "CAZ_REPORTING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEntrantReporting {

  /**
   * Default constructor for clean air zone entrant entity.
   */
  public VehicleEntrantReporting(UUID cleanAirZoneId, String vrnHash,
      LocalDateTime hour, String chargeValidityCode, String make,
      String model, String colour, boolean nonUkVehicle) {
    this.cleanAirZoneId = cleanAirZoneId;
    this.vrnHash = vrnHash;
    this.hour = hour;
    this.chargeValidityCode = chargeValidityCode;
    this.make = make;
    this.model = model;
    this.colour = colour;
    this.nonStandardUkPlateFormatVehicle = nonUkVehicle;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "vehicle_entrant_reporting_id")
  private UUID vehicleEntrantReportingId;
  
  @Column(name = "vrn_hash")
  private String vrnHash;

  @Column(name = "hour")
  private LocalDateTime hour;

  @Column(name = "charge_validity_code")
  private String chargeValidityCode;

  @Column(name = "clean_air_zone_id")
  private UUID cleanAirZoneId;

  @Setter
  @Column(name = "type_approval_id")
  private UUID typeApprovalId;
  
  @Setter
  @Column(name = "fuel_type_id")
  private UUID fuelTypeId;
  
  @Setter
  @Column(name = "ccaz_vehicle_type_id")
  private UUID ccazVehicleTypeId;

  @Column(name = "make")
  private String make;

  @Column(name = "model")
  private String model;
  
  @Column(name = "colour")
  private String colour;
  
  @Column(name = "non_standard_uk_plate_format_vehicle")
  private boolean nonStandardUkPlateFormatVehicle;
}
