package uk.gov.caz.accounts.model;

import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity over T_VEHICLE_CHARGEABILITY table.
 */
@AllArgsConstructor
@Builder
@Entity
@NoArgsConstructor
@Table(schema = "caz.account", name = "t_vehicle_chargeability")
@IdClass(VehicleChargeabilityId.class)
public class VehicleChargeability {

  /**
   * Part of composite primary key - vehicle account id.
   */
  @Column(name = "account_vehicle_id")
  @Id
  @Getter
  private UUID accountVehicleId;

  /**
   * Part of composite primary key - CAZ id.
   */
  @Column(name = "caz_id")
  @Id
  @Getter
  private UUID cazId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_vehicle_id", insertable = false, updatable = false)
  private AccountVehicle accountVehicle;

  /**
   * Nullable charge for vehicle in specified CAZ.
   */
  @Column(name = "charge")
  @Getter
  private BigDecimal charge;

  /**
   * Exemption status for vehicle in specified CAZ.
   */
  @Getter
  @Column(name = "is_exempt")
  private boolean isExempt;

  /**
   * Retrofitted status for vehicle in specified CAZ.
   */
  @Getter
  @Column(name = "is_retrofitted")
  private boolean isRetrofitted;

  /**
   * Tariff code for vehicle in specified CAZ.
   */
  @Column(name = "tariff_code")
  @Getter
  private String tariffCode;
}
