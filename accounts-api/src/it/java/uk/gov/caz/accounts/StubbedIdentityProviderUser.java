package uk.gov.caz.accounts;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Value class used to collect user details in StubbedIdentityProvider.
 */
@Value
@Builder(toBuilder = true)
public class StubbedIdentityProviderUser {

  /**
   * User's email address.
   */
  String email;

  /**
   * User's password.
   */
  String password;

  /**
   * User's name - only for a standard user (isOwner is false).
   */
  String name;

  /**
   * The external Identity Provider User identifier. USER_ID from table ACCOUNT_USER.
   */
  UUID identityProviderUserId;

  UUID accountId;

  /**
   * true = email has been verified.
   */
  boolean emailVerified;

  boolean isOwner;

  int failedLogins;

  LocalDateTime lockoutTime;

}
