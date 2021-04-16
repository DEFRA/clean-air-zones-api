package uk.gov.caz.accounts.model;

import java.util.Date;
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
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(schema = "caz_account", name = "t_account_direct_debit_mandate")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DirectDebitMandate {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "direct_debit_mandate_id")
  UUID id;

  @Column(name = "account_id")
  UUID accountId;

  @Column(name = "clean_air_zone_id")
  @NonNull
  UUID cleanAirZoneId;

  @Column(name = "account_user_id")
  UUID accountUserId;

  @Column(name = "payment_provider_mandate_id")
  @NonNull
  String paymentProviderMandateId;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  DirectDebitMandateStatus status;

  @Column(name = "created")
  @CreationTimestamp
  Date created;
}
