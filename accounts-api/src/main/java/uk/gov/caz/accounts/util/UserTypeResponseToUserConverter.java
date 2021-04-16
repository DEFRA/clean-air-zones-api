package uk.gov.caz.accounts.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;

/**
 * Utility class which returns User object based on a {@code UserType} which comes as a response
 * from a third-party service.
 */
@UtilityClass
@Slf4j
public class UserTypeResponseToUserConverter {

  /**
   * Method converting {@code UserType} to a {@code User}.
   *
   * @param user object containing user details from DB
   * @param response response from a third party service.
   * @return User model
   * @throws IdentityProviderUnavailableException when email attribute is not found in
   *     attributes list.
   */
  public static User convert(User user, UserType response) {
    return user.toBuilder()
        .name(getAttributeFromResponse(response, "name"))
        .email(getAttributeFromResponse(response, "email"))
        .build();
  }

  /**
   * Method converting {@code UserType} to a {@code UserEntity}.
   *
   * @param user object containing user details from DB
   * @param response response from a third party service.
   * @return UserEntity model
   * @throws IdentityProviderUnavailableException when email attribute is not found in
   *     attributes list.
   */
  public static UserEntity convert(UserEntity user, UserType response) {
    return user.toBuilder()
        .name(getAttributeFromResponse(response, "name"))
        .email(getAttributeFromResponse(response, "email"))
        .build();
  }

  /**
   * Method which extracts proper attribute from identity provider response.
   *
   * @param userType response from a third party service
   * @param attributeName attribute which is going to be extracted from the response
   * @return String containing expected attribute or {@code null} if not found
   */
  private static String getAttributeFromResponse(UserType userType,
      String attributeName) {
    return userType.attributes().stream()
        .filter(attribute -> attribute.name().equals(attributeName))
        .findFirst()
        .map(AttributeType::value)
        .orElse(null);
  }
}
