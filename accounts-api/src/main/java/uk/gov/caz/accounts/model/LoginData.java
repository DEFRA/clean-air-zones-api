package uk.gov.caz.accounts.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

/**
 * Value object that contains the successful login results.
 */
@Value(staticConstructor = "of")
public class LoginData {

  /**
   * User which has been authenticated.
   */
  @NonNull
  UserEntity user;

  /**
   * Account linked to the {@link LoginData#user}.
   */
  @NonNull
  Account account;

  /**
   * Timestamp of when was the last password update (or user creation if not yet known).
   */
  @NonNull
  LocalDateTime passwordUpdateTimestamp;

  /**
   * List of permissions for given user.
   */
  @NonNull
  List<Permission> permissions;

  /**
   * Stores information if user is beta tester.
   */
  boolean betaTester;
}
