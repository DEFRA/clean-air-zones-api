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
 * Model that represents Reporting data on exempt vehicles.
 */
@Entity
@Table(name = "t_entrant_exemption", schema = "CAZ_REPORTING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntrantExemption {
  
  /**
   * Default constructor for entrant exemption entity.
   * @param vehicleEntrantReportingId Id for the {@VehicleEntrantReporting} instance
   * @param exemptionId Id for the exemptionReason
   */
  public EntrantExemption(UUID vehicleEntrantReportingId, UUID exemptionId) {
    this.vehicleEntrantReportingId = vehicleEntrantReportingId;
    this.exemptionReasonId = exemptionId;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "entrant_exemption_id")
  private UUID entrantTaxiPhvId;

  @Column(name = "vehicle_entrant_reporting_id")
  private UUID vehicleEntrantReportingId;

  @Column(name = "exemption_reason_id")
  private UUID exemptionReasonId;
}
