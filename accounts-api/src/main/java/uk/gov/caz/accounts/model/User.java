package uk.gov.caz.accounts.model;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Value class used to collect user details.
 */
@Value
@Builder(toBuilder = true)
public class User {

  /**
   * Takes two {@link User} instances, one coming from our DB and the other from Identity provider
   * then combines them into one fully initialized {@link User} object.
   *
   * @param userFromDb {@link User} from our DB.
   * @param userFromIdentityProvider {@link User} from Identity provider.
   * @return Fully initialized {@link User} object that can safely be used across our services.
   */
  public static User combinedDbAndIdentityProvider(User userFromDb, User userFromIdentityProvider) {
    return userFromDb.toBuilder()
        .email(userFromIdentityProvider.getEmail())
        .name(userFromIdentityProvider.getName())
        .emailVerified(userFromIdentityProvider.isEmailVerified()).build();
  }

  /**
   * The internal unique account_user identifier. ACCOUNT_USER_ID from table ACCOUNT_USER.
   */
  UUID id;

  /**
   * The internal unique account identifier.
   */
  @With
  UUID accountId;

  /**
   * The external Identity Provider User identifier. USER_ID from table ACCOUNT_USER.
   */
  @With
  UUID identityProviderUserId;

  /**
   * Variable which determines if user is an owner (fleet manager).
   */
  boolean isOwner;

  /**
   * User's email address.
   */
  String email;

  /**
   * User's name (for non admin only).
   */
  @Nullable
  String name;

  /**
   * Variable which determines if user verified email in Identity Provider.
   */
  boolean emailVerified;

  /**
   * Identifier a of a user who invited this user.
   */
  UUID administeredBy;

  /**
   * List of account permission names assigned to User
   * Is loaded only when getting Standard User details.
   */
  List<String> accountPermissions;

  /**
   * Helper method to check if user is removed.
   *
   * @return boolean value.
   */
  public boolean isRemoved() {
    return identityProviderUserId == null;
  }

  /**
   * An overridden lombok's builder.
   */
  public static class UserBuilder {

    public UserBuilder owner() {
      this.isOwner = true;
      return this;
    }
  }
}
