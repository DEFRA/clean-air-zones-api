package uk.gov.caz.accounts.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Class that holds data for given user.
 */
@Value
@Builder(toBuilder = true)
public class UserDetailsResponse {

  /**
   * Id of the account for this user.
   */
  UUID accountId;

  /**
   * Name of the account for this user.
   */
  String accountName;

  /**
   * Id of the user.
   */
  UUID accountUserId;

  /**
   * Name of the User from the Identity Provider.
   */
  String name;

  /**
   * Email of the User from the Identity Provider.
   */
  String email;

  /**
   * Variable which determines if user is an owner (fleet manager).
   */
  boolean owner;

  /**
   * Variable which determines if user is removed.
   */
  boolean removed;

  /**
   * Method that converts {@link UserEntity} and {@link Account} to {@link UserDetailsResponse}.
   */
  public static UserDetailsResponse from(UserEntity user, Account account) {
    return UserDetailsResponse.builder()
        .accountId(user.getAccountId())
        .accountName(account.getName())
        .accountUserId(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .removed(user.isRemoved())
        .owner(user.isOwner())
        .build();
  }
}
