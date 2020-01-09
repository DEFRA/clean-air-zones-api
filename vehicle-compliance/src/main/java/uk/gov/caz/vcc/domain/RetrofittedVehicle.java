package uk.gov.caz.vcc.domain;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a vehicle which has been retrofitted.
 *
 * @author Informed Solutions
 */
@Entity
@Table(name = "t_vehicle_retrofit")
public class RetrofittedVehicle {

  @Id
  @Getter
  @Setter
  String vrn;

  @Column(name = "date_of_retrofit")
  @Getter
  @Setter
  Date dateOfRetrofit;

  @Column(name = "whitelist_discount_code")
  @Getter
  @Setter
  String whitelistDiscountCode;
}
