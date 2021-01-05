package uk.gov.caz.accounts.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;

/**
 * Utility class which returns User object based on a {@code AdminGetUserResponse} which comes as a
 * response from a third-party service.
 */
@UtilityClass
@Slf4j
public class AdminGetUserResponseToUserConverter {

  /**
   * Method converting {@code AdminGetUserResponse} to a {@code User}.
   *
   * @param response response from a third party service.
   * @return User model
   * @throws IdentityProviderUnavailableException when email attribute is not found in
   *     attributes list.
   */
  public static User convert(AdminGetUserResponse response) {
    AttributeType retrievedEmail = getAttributeFromResponse(response, "email");
    AttributeType retrievedIdentityProviderId = getAttributeFromResponse(response,
        "preferred_username");
    Optional<AttributeType> retrievedName = getOptionalAttributeFromResponse(response, "name");

    return User.builder()
        .name(retrievedName.map(AttributeType::value).orElse(null))
        .email(retrievedEmail.value())
        .identityProviderUserId(UUID.fromString(retrievedIdentityProviderId.value()))
        .emailVerified(retrieveEmailVerified(response))
        .build();
  }

  /**
   * Creates an instance of {@link UserEntity} with {@code userId}, {@code email} and {@code
   * emailVerified} attributes set.
   */
  public static UserEntity convertToUserEntity(AdminGetUserResponse response) {
    AttributeType email = getAttributeFromResponse(response, "email");
    AttributeType identityProviderId = getAttributeFromResponse(response, "preferred_username");
    Optional<AttributeType> retrievedName = getOptionalAttributeFromResponse(response, "name");
    Optional<AttributeType> failedLogins = getOptionalAttributeFromResponse(response,
        "custom:failed-logins");
    Optional<AttributeType> lockoutTime = getOptionalAttributeFromResponse(response,
        "custom:lockout-time");
    Optional<AttributeType> passwordUpdateTimestamp = getOptionalAttributeFromResponse(response,
        "custom:password-lu-tstmp");

    return UserEntity.builder()
        .name(retrievedName.map(AttributeType::value).orElse(null))
        .identityProviderUserId(UUID.fromString(identityProviderId.value()))
        .email(email.value())
        .emailVerified(retrieveEmailVerified(response))
        .failedLogins(failedLogins.map(AttributeType::value)
            .map(Integer::valueOf)
            .orElse(0))
        .lockoutTime(lockoutTime.map(AttributeType::value)
            .map(AdminGetUserResponseToUserConverter::parseLockoutTimestamp)
            .orElse(null))
        .passwordUpdateTimestamp(
            passwordUpdateOrIfNotPresentUserCreateDate(passwordUpdateTimestamp,
                response.userCreateDate()))
        .build();
  }

  /**
   * Extracts timestamp when password was last updated or if not yet known gets user creation
   * timestamp.
   */
  private static LocalDateTime passwordUpdateOrIfNotPresentUserCreateDate(
      Optional<AttributeType> passwordUpdateTimestamp, Instant userCreationTimestamp) {
    return passwordUpdateTimestamp.map(put -> LocalDateTime.parse(put.value()))
        .orElseGet(() -> LocalDateTime.ofInstant(userCreationTimestamp,
            ZoneOffset.UTC));
  }

  /**
   * Method which extracts proper attribute from identity provider response.
   *
   * @param response response from a third party service
   * @param attributeName attribute which is going to be extracted from the response
   * @return AttributeType containing expected attribute
   */
  public static AttributeType getAttributeFromResponse(AdminGetUserResponse response,
      String attributeName) {
    Optional<AttributeType> retrievedAttribute = getOptionalAttributeFromResponse(response,
        attributeName);

    if (!retrievedAttribute.isPresent()) {
      log.error("Cannot find {} property in Identity Service response", attributeName);
      throw new IdentityProviderUnavailableException("External Service Failure");
    }

    return retrievedAttribute.get();
  }

  /**
   * Method which tries to extracts proper attribute from identity provider response.
   *
   * @param response response from a third party service
   * @param attributeName attribute which is going to be extracted from the response
   * @return Optional AttributeType containing expected attribute.
   */
  public static Optional<AttributeType> getOptionalAttributeFromResponse(
      AdminGetUserResponse response, String attributeName) {
    return response
        .userAttributes()
        .stream()
        .filter(attributeType -> attributeType.name().equals(attributeName))
        .findFirst();
  }

  /**
   * Method which extracts emailVerified variable from identity provider response.
   *
   * @param response response from a third party service
   * @return boolean value which represent if email was verified
   */
  private boolean retrieveEmailVerified(AdminGetUserResponse response) {
    return response.userAttributes()
        .stream()
        .filter(attributeType -> attributeType.name().equals("email_verified"))
        .filter(attributeType -> "true".equalsIgnoreCase(attributeType.value()))
        .map(attributeType -> Boolean.TRUE)
        .findFirst()
        .orElse(Boolean.FALSE);
  }

  /**
   * Parses the lockout time attribute present at cognito to  an instance of {@link LocalDateTime}.
   */
  private static LocalDateTime parseLockoutTimestamp(String currentLockoutTime) {
    if ("".equals(currentLockoutTime)) {
      return null;
    }
    return LocalDateTime.parse(currentLockoutTime);
  }
}
