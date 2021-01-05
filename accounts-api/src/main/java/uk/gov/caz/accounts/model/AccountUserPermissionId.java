package uk.gov.caz.accounts.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Class representing a composite primary key for {@code T_ACCOUNT_USER_PERMISSION} table.
 **/
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AccountUserPermissionId implements Serializable {

  private static final long serialVersionUID = -9120163410565075534L;

  Long accountPermissionId;

  UUID accountUserId;
}
