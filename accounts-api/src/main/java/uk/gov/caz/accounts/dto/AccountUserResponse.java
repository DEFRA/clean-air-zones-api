package uk.gov.caz.accounts.dto;

import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Class that represents the JSON structure for  response.
 */
@Value
@Builder
public class AccountUserResponse {

  /**
   * Id of User in DB.
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
   * Boolean specifying whether the user is an owner.
   */
  boolean owner;

  /**
   * Boolean specifying whether the user is removed.
   */
  boolean removed;

  /**
   * Creates {@link AccountUserResponse} object from passed {@link UserEntity} object.
   */
  public static AccountUserResponse from(UserEntity user) {
    return AccountUserResponse.builder()
        .accountUserId(user.getId())
        .name(Optional.ofNullable(user.getName()).orElse(""))
        .email(Optional.ofNullable(user.getEmail()).orElse(""))
        .owner(user.isOwner())
        .removed(user.isRemoved())
        .build();
  }
}
