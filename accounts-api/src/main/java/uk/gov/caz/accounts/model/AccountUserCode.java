package uk.gov.caz.accounts.model;

import java.time.LocalDateTime;
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

/**
 * A database entity which represents a account user code.
 */
@Entity
@Table(schema = "caz_account", name = "t_account_user_code")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountUserCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_user_code_id")
  Integer id;

  @NonNull
  @Column(name = "account_user_id")
  UUID accountUserId;

  @NonNull
  String code;

  @NonNull
  LocalDateTime expiration;

  @NonNull
  @Enumerated(EnumType.STRING)
  CodeType codeType;

  @NonNull
  @Enumerated(EnumType.STRING)
  CodeStatus status;

  /**
   * Helper method to check if the code is active.
   *
   * @return return boolean value.
   */
  public boolean isActive() {
    return status.equals(CodeStatus.ACTIVE) && !isExpired();
  }

  /**
   * Helper method to check if the code is expired.
   *
   * @return return boolean value.
   */
  public boolean isExpired() {
    return expiration.isBefore(LocalDateTime.now());
  }
}
