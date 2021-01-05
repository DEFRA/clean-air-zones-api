package uk.gov.caz.accounts.util;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Utility class which returns User object based on a {@code UserEntity}.
 */
@UtilityClass
public class UserEntityToUserConverter {
  /**
   * Method converting {@code UserType} to a {@code User}.
   *
   * @param userEntity object containing user details from DB
   * @return User model
   */
  public static User convert(UserEntity userEntity) {
    List<String> permissions = userEntity.getAccountPermissions().stream()
        .map(accountPermission -> accountPermission.getName().toString())
        .collect(Collectors.toList());
    return User.builder()
        .id(userEntity.getId())
        .identityProviderUserId(userEntity.getIdentityProviderUserId())
        .accountPermissions(permissions)
        .build();
  }
}
