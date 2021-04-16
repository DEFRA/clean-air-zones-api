package uk.gov.caz.vcc.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.Length;

/**
 * Model that represents Charge Validity data.
 */
@Entity
@Getter
@Table(name = "t_charge_validity", schema = "CAZ_VEHICLE_ENTRANT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChargeValidity {

  public ChargeValidity(String chargeValidityCode) {
    this.chargeValidityCode = chargeValidityCode;
  }

  @Id
  @Length(max = 5)
  @Column(name = "charge_validity_code")
  private String chargeValidityCode;

  @Setter
  @Column(name = "validity_code_desc")
  private String validityCodeDesc;

  @CreationTimestamp
  @Column(name = "insert_timestmp")
  private LocalDateTime insertTimestamp;
}
