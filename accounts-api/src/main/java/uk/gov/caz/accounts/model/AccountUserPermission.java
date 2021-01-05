package uk.gov.caz.accounts.model;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * A database entity which represents a row in a link T_ACCOUNT_USER_PERMISSION table.
 */
@Entity
@Table(schema = "CAZ_ACCOUNT", name = "T_ACCOUNT_USER_PERMISSION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AccountUserPermissionId.class)
public class AccountUserPermission {

  @Id
  @Column(name = "ACCOUNT_PERMISSION_ID")
  @NonNull
  Long accountPermissionId;

  @Id
  @Column(name = "ACCOUNT_USER_ID")
  @NonNull
  UUID accountUserId;
}
