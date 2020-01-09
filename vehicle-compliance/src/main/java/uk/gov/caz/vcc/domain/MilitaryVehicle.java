package uk.gov.caz.vcc.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Domain object to represent a MOD vehicle.
 *
 * @author Informed Solutions
 */
@Entity
@Table(name = "t_mod_whitelist")
public class MilitaryVehicle {

  @Id
  @Getter
  @Setter
  String vrn;

  @Column(name = "mod_whitelist_type")
  @Getter
  @Setter
  String modWhitelistType;

  @Column(name = "whitelist_discount_code")
  @Getter
  @Setter
  String whitelistDiscountCode;
}
