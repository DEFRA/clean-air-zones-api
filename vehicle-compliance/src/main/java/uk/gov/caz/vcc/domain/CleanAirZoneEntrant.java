package uk.gov.caz.vcc.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

/**
 * Model that represents single entrant to the Clean Air Zone.
 */
@Entity
@Getter
@Table(name = "t_clean_air_zone_entrant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CleanAirZoneEntrant {

  /**
   * Default constructor for clean air zone entrant entity.
   */
  public CleanAirZoneEntrant(UUID cleanAirZoneId, String correlationId,
      LocalDateTime entrantTimestamp) {
    this.cleanAirZoneId = cleanAirZoneId;
    this.correlationId = correlationId;
    this.entrantTimestamp = entrantTimestamp;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "entrant_id")
  private int entrantId;

  @Setter
  @ManyToOne
  @JoinColumn(name = "charge_validity_code")
  private ChargeValidity chargeValidityCode;

  @NotNull
  @Column(name = "clean_air_zone_id")
  private UUID cleanAirZoneId;

  @NotNull
  @Length(max = 40)
  @Column(name = "correlation_id")
  private String correlationId;

  @Length(max = 15)
  @Setter
  private String vrn;

  @Column(name = "entrant_timestmp")
  private LocalDateTime entrantTimestamp;

  @CreationTimestamp
  @Column(name = "insert_timestmp")
  private LocalDateTime insertTimestamp;
}
