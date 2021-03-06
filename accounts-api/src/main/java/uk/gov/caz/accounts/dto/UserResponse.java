package uk.gov.caz.accounts.dto;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Class that represents the JSON structure for  response.
 */
@Value
@Builder
public class UserResponse {

  /**
   * Name of the User from the Identity Provider.
   */
  String name;

  /**
   * Email of the User from the Identity Provider.
   */
  String email;

  /**
   * List of user account permissions.
   */
  List<String> permissions;

  /**
   * Variable which determines if user is an owner (fleet manager).
   */
  boolean owner;

  /**
   * Creates {@link UserResponse} object from passed {@link UserEntity} object.
   */
  public static UserResponse from(UserEntity user) {
    List<String> permissions = user.getAccountPermissions().stream()
        .map(accountPermission -> accountPermission.getName().toString())
        .collect(Collectors.toList());

    return UserResponse.builder()
        .name(user.getName())
        .email(user.getEmail())
        .owner(user.isOwner())
        .permissions(permissions)
        .build();
  }
}
