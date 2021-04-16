package uk.gov.caz.vcc.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model that represents a vehicle saved to the General Whitelist service
 * for exemption or charge alteration purposes.
 */
@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_whitelist_vehicles", schema = "caz_whitelist_vehicles")
public class GeneralWhitelistVehicle {

  @Id
  private String vrn;

  @Column(name = "REASON_UPDATED")
  private String reasonUpdated;

  @Column(name = "UPDATE_TIMESTAMP")
  private LocalDateTime updateTimestamp;

  @Column(name = "UPLOADER_ID")
  private UUID uploaderId;

  @Column(name = "CATEGORY")
  private String category;

  @Column(name = "EXEMPT")
  private boolean exempt;

  @Column(name = "COMPLIANT")
  private boolean compliant;

}
