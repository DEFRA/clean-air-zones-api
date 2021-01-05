package uk.gov.caz.accounts.model;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Entity
@NoArgsConstructor
@Table(schema = "caz.account", name = "t_account_vehicle")
@Getter
public class AccountVehicle {

  @Column(name = "account_vehicle_id")
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID accountVehicleId;

  @Column(name = "account_id")
  private UUID accountId;

  private String vrn;

  @Column(name = "caz_vehicle_type")
  private String cazVehicleType;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "accountVehicle")
  List<VehicleChargeability> vehicleChargeability;
}
