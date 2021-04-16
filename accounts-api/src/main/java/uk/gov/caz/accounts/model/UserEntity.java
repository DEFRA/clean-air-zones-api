package uk.gov.caz.accounts.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "CAZ_ACCOUNT", name = "T_ACCOUNT_USER")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "account_user_id", unique = true)
  UUID id;

  @Column(name = "account_id")
  UUID accountId;

  @Column(name = "is_owner")
  boolean isOwner;

  @Column(name = "user_id", unique = true)
  UUID identityProviderUserId;

  @Column(name = "last_sign_in_timestmp")
  @Nullable
  Timestamp lastSingInTimestmp;

  /**
   * Stores the external identifier of a user created solely in cognito - used in 'change email'
   * feature when a user wants to change his email address.
   */
  @Column(name = "pending_user_id")
  UUID pendingUserId;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      schema = "CAZ_ACCOUNT",
      name = "T_ACCOUNT_USER_PERMISSION",
      joinColumns = @JoinColumn(name = "ACCOUNT_USER_ID", referencedColumnName = "ACCOUNT_USER_ID"),
      inverseJoinColumns = @JoinColumn(name = "ACCOUNT_PERMISSION_ID",
          referencedColumnName = "ACCOUNT_PERMISSION_ID"))
  List<AccountPermission> accountPermissions;

  // only for non-owner users (invited)
  @Transient
  String name;

  @Transient
  String email;

  @Transient
  boolean emailVerified;

  @Column(name = "is_administrated_by")
  UUID isAdministratedBy;

  @Transient
  int failedLogins;

  @Transient
  LocalDateTime lockoutTime;

  @Transient
  LocalDateTime passwordUpdateTimestamp;

  public Optional<LocalDateTime> getLockoutTime() {
    return Optional.ofNullable(lockoutTime);
  }

  public Optional<UUID> getPendingUserId() {
    return Optional.ofNullable(pendingUserId);
  }

  public boolean isRemoved() {
    return Objects.isNull(identityProviderUserId);
  }

  /**
   * Check if user has {@code Permission.MANAGE_VEHICLES} assigned.
   */
  public boolean hasVehicleManagementPermission() {
    return !getAccountPermissions().stream()
        .filter(accountPermission -> accountPermission.getName().equals(Permission.MANAGE_VEHICLES))
        .collect(Collectors.toList())
        .isEmpty();
  }
}
