package uk.gov.caz.accounts.model;

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

/**
 * A database entity which represents a account user code.
 */
@Entity
@Table(schema = "CAZ_ACCOUNT", name = "T_ACCOUNT_PERMISSION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountPermission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ACCOUNT_PERMISSION_ID")
  Long id;

  @NonNull
  @Column(name = "NAME")
  @Enumerated(EnumType.STRING)
  Permission name;

  @NonNull
  @Column(name = "DESCRIPTION")
  String description;

}
