package uk.gov.caz.accounts.model;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "CAZ_ACCOUNT", name = "T_ACCOUNT")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "account_id", unique = true)
  UUID id;

  @Column(name = "account_name")
  String name;

  @Column(name = "closure_reason")
  @Enumerated(EnumType.STRING)
  AccountClosureReason closureReason;

  @Column(name = "inactivation_tstamp")
  LocalDateTime inactivationTimestamp;

  @Column(name = "multi_payer_account")
  boolean multiPayerAccount;

  /**
   * Method returns information if the account is active or inactivated.
   *
   * @return boolean
   */
  public boolean isActive() {
    return !getInactivationTimestamp().isPresent();
  }

  /**
   * Method returns optional inactivationTimestamp of the account.
   *
   * @return Optional with inactivationTimestamp
   */
  public Optional<LocalDateTime> getInactivationTimestamp() {
    return Optional.ofNullable(inactivationTimestamp);
  }

}
