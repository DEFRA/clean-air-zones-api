package uk.gov.caz.vcc.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Model that represents Reporting data on taxis.
 */
@Entity
@Table(name = "t_entrant_taxi_phv", schema = "CAZ_REPORTING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntrantTaxiPhv {
  
  /**
   * Default constructor for clean air zone entrant entity.
   * @param vehicleEntrantReportingId Id for the {@VehicleEntrantReporting} 
   * @param description taxi/phv description
   * @param licensingAuthority taxi/phv licensingAuthority
   */
  public EntrantTaxiPhv(UUID vehicleEntrantReportingId, String description,
      String licensingAuthority) {
    this.vehicleEntrantReportingId = vehicleEntrantReportingId;
    this.description = description; 
    this.licensingAuthority = licensingAuthority;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "entrant_taxi_phv_id")
  private UUID entrantTaxiPhvId;

  @Column(name = "vehicle_entrant_reporting_id")
  private UUID vehicleEntrantReportingId;

  @Column(name = "description")
  private String description;
  
  @Column(name = "licensing_authority")
  private String licensingAuthority;
}
