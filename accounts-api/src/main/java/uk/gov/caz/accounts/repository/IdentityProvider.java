package uk.gov.caz.accounts.repository;

import static uk.gov.caz.accounts.util.auth.SecretHashCalculator.calculateSecretHash;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserSettingsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.service.exception.EmailNotConfirmedException;
import uk.gov.caz.accounts.service.exception.PasswordInvalidException;
import uk.gov.caz.accounts.util.AdminGetUserResponseToUserConverter;
import uk.gov.caz.accounts.util.Sha2Hasher;
import uk.gov.caz.accounts.util.Strings;
import uk.gov.caz.accounts.util.UserTypeResponseToUserConverter;

/**
 * Class with low level operations related to managing user identities, for example sign-up, login,
 * email verification etc.
 */
@AllArgsConstructor
@Slf4j
public class IdentityProvider {

  private static final String EMAIL_ATTRIBUTE = "email";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String IDENTITY_PROVIDER_ID_ATTRIBUTE = "preferred_username";
  private static final String FAILED_LOGINS_ATTRIBUTE = "custom:failed-logins";
  private static final String LOCKOUT_TIME_ATTRIBUTE = "custom:lockout-time";
  private static final String PREVIOUS_PASSWORDS_ATTRIBUTE = "custom:previous-passwords";
  private static final String PASSWORD_LAST_UPDATE_TIMESTAMP_ATTRIBUTE =
      "custom:password-lu-tstmp";
  private static final String PREVIOUS_PASSWORDS_DELIMITER = ",";
  private static final int MAX_PREVIOUS_PASSWORDS = 5;
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  private final String userPoolId;
  private final String appClientId;
  private final String appClientSecret;

  private final CognitoIdentityProviderClient cognitoClient;

  /**
   * Gets value of user pool id.
   *
   * @return user pool id.
   */
  public String getUserPoolId() {
    return userPoolId;
  }

  /**
   * Gets user's email from the external identity provider based on the provided external
   * identifier.
   *
   * @param identityProviderId User ID in IdentityProvider.
   * @return found email.
   * @throws IdentityProviderUnavailableException if user not found.
   */
  public String getEmailByIdentityProviderId(UUID identityProviderId) {
    ListUsersRequest listUsersRequest = buildListUsersByIdentityProviderRequest(identityProviderId);

    ListUsersResponse response = cognitoClient.listUsers(listUsersRequest);
    logResponse(response);

    if (response.users().isEmpty()) {
      log.error("Cannot find user in Identity provider for identityProviderId: '{}'",
          identityProviderId);
      throw new IdentityProviderUnavailableException();
    }

    return getCognitoUserAttribute(response.users().iterator().next(), EMAIL_ATTRIBUTE);
  }

  /**
   * Gets user's details from the external identity provider based on the provided external
   * identifier.
   *
   * @param user User object containing details from DB
   * @return {@link User} with collected email and name.
   * @throws IdentityProviderUnavailableException if user not found.
   * @throws NullPointerException if user identityProviderUserId is null.
   */
  public User getUserDetailsByIdentityProviderId(User user) {
    Preconditions.checkNotNull(user.getIdentityProviderUserId(),
        "'identityProviderUserId' cannot be null");

    ListUsersRequest listUsersRequest = buildListUsersByIdentityProviderRequest(
        user.getIdentityProviderUserId());

    ListUsersResponse response = cognitoClient.listUsers(listUsersRequest);
    logResponse(response);

    if (response.users().isEmpty()) {
      log.error("Cannot find user in Identity provider for identityProviderUserId: '{}'",
          user.getIdentityProviderUserId());
      throw new IdentityProviderUnavailableException();
    }

    return UserTypeResponseToUserConverter.convert(user, response.users().iterator().next());
  }

  /**
   * Gets user's details from the external identity provider based on the provided external
   * identifier.
   *
   * @param email String with User registered email.
   * @return boolean value.
   * @throws IllegalArgumentException when {@code email} is empty.
   */
  public boolean isUserBetaTester(String email) {
    Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(email),
        "'email' cannot be null or empty");

    AdminListGroupsForUserRequest requestAttributes = AdminListGroupsForUserRequest.builder()
        .username(email)
        .userPoolId(userPoolId)
        .build();

    AdminListGroupsForUserResponse userGroups = cognitoClient
        .adminListGroupsForUser(requestAttributes);

    return userGroups.groups().stream()
        .anyMatch(g -> g.groupName().matches("accounts\\.beta-testers\\..*"));
  }

  /**
   * Method makes a call to the third-party service in order to check if an account with provided
   * email already exists.
   *
   * @param email which is going to be verified.
   * @return boolean value.
   */
  public boolean checkIfUserExists(String email) {
    ListUsersRequest listUsersRequest = buildListUsersByEmailRequest(email);

    ListUsersResponse response = cognitoClient.listUsers(listUsersRequest);
    logResponse(response);

    return !response.users().isEmpty();
  }

  /**
   * Method makes a call to the third-party service in order to fetch account details.
   *
   * @param email parameter used to fetch User data
   * @return User value object
   */
  public User getUser(String email) {
    AdminGetUserRequest adminGetUserRequest = buildAdminGetUserRequest(email);

    AdminGetUserResponse response = cognitoClient.adminGetUser(adminGetUserRequest);
    logResponse(response);

    return AdminGetUserResponseToUserConverter.convert(response);
  }

  /**
   * Method makes a call to the third-party service in order to fetch account details.
   *
   * @param email parameter used to fetch User data
   * @return An instance of {@link UserEntity}
   */
  public UserEntity getUserAsUserEntity(String email) {
    AdminGetUserRequest adminGetUserRequest = buildAdminGetUserRequest(email);

    AdminGetUserResponse response = cognitoClient.adminGetUser(adminGetUserRequest);
    logResponse(response);

    return AdminGetUserResponseToUserConverter.convertToUserEntity(response);
  }

  /**
   * Method makes a call to the third-party service in order to fetch user's details.
   *
   * @param identityProviderUserId An external identifier of a user
   * @return An instance of {@link UserEntity}
   */
  public UserEntity getUserAsUserEntityByExternalId(UUID identityProviderUserId) {
    String email = getEmailByIdentityProviderId(identityProviderUserId);
    return getUserAsUserEntity(email);
  }

  /**
   * Method to log cognito response.
   *
   * @param response {@link CognitoIdentityProviderResponse}
   */
  private void logResponse(CognitoIdentityProviderResponse response) {
    log.info("Received response: {}", response);
  }

  /**
   * Helper method to build {@link ListUsersRequest}.
   *
   * @param identityProviderId used to filter the users.
   * @return {@link ListUsersRequest} with provided identityProviderId.
   */
  private ListUsersRequest buildListUsersByIdentityProviderRequest(UUID identityProviderId) {
    return ListUsersRequest.builder()
        .filter(IDENTITY_PROVIDER_ID_ATTRIBUTE + " = \"" + identityProviderId + "\"")
        .limit(1)
        .userPoolId(userPoolId)
        .build();
  }

  /**
   * Helper method to build {@link ListUsersRequest}.
   *
   * @param email used to filter the users.
   * @return {@link ListUsersRequest} with provided email.
   */
  public ListUsersRequest buildListUsersByEmailRequest(String email) {
    return ListUsersRequest.builder()
        .attributesToGet(EMAIL_ATTRIBUTE)
        .filter("email = \"" + email + "\"")
        .limit(1)
        .userPoolId(userPoolId)
        .build();
  }

  /**
   * Helper method to build {@link AdminGetUserRequest}.
   *
   * @param email email used to identify the user
   * @return {@link AdminGetUserRequest} with email and userPoolId params
   */
  private AdminGetUserRequest buildAdminGetUserRequest(String email) {
    return AdminGetUserRequest.builder()
        .userPoolId(userPoolId)
        .username(email)
        .build();
  }

  /**
   * Method to create admin user in identity provider.
   *
   * @param email provided user email.
   * @param password provided user password.
   * @throws IllegalArgumentException when {@code password} or {@code email} is empty.
   * @throws IdentityProviderUnavailableException upon any cognito exception
   */
  public void createAdminUser(UUID identityProviderId, String email, String password) {
    Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(email),
        "'email' cannot be null or empty");
    Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(password),
        "'password' cannot be null or empty");

    createCognitoAdminUser(identityProviderId, email, password);
  }

  /**
   * Creates an admin user in Cognito and sets their password so that the account is enabled, but
   * NOT email-confirmed. Email-confirmed attribute ("email_verified") must be explicitly set via an
   * API call that uses {@link #verifyEmail(User)}. {@code cognitoClient#adminCreateUser} method
   * must be used to suppress the confirmation code being sent (this cannot be done by using {@code
   * cognitoClient#signUp}). See the <a href="https://docs.aws.amazon.com/cognito/latest/developerguide/signing-up-users-in-your-app.html#signup-confirmation-verification-overview">documentation</a>
   * for possible states the account can be in.
   *
   * @param email email that needs to be signed up in Cognito.
   * @param password email that needs to be signed up in Cognito.
   */
  private void createCognitoAdminUser(UUID identityProviderId, String email, String password) {
    try {
      AdminCreateUserResponse cognitoUser = cognitoClient.adminCreateUser(
          createAdminUserRequest(identityProviderId, email, password));
      setUserPassword(email, password);
      log.info("Successfully created a user in cognito: {}", cognitoUser);
    } catch (InvalidPasswordException | InvalidParameterException exception) {
      logAwsExceptionDetails(exception);
      throw new PasswordInvalidException("Parameters cannot be processed");
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Throws {@link IllegalStateException} unless {@link AdminInitiateAuthResponse#challengeName()}
   * is {@code null}.
   */
  private void verifyNullChallengeNameIn(AdminInitiateAuthResponse response) {
    if (response.challengeName() != null) {
      throw new IllegalStateException("No challenges expected, actual '"
          + response.challengeName() + "'");
    }
  }

  /**
   * Creates an instance of {@link AdminCreateUserRequest} based on the passed params.
   */
  private AdminCreateUserRequest createAdminUserRequest(UUID identityProviderId,
      String email, String password) {
    return baseAdminCreateUserBuilder(identityProviderId, email)
        .temporaryPassword(password)
        .messageAction(MessageActionType.SUPPRESS)
        .build();
  }

  /**
   * Method to delete admin user in identity provider.
   *
   * @param email of user who should be deleted
   * @throws IdentityProviderUnavailableException when Identity Provider fails.
   */
  public void deleteUser(String email) {
    try {
      if (checkIfUserExists(email)) {
        cognitoClient.adminDeleteUser(
            AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build());
      }
    } catch (CognitoIdentityProviderException exception) {
      log.error("Unable to delete user: '{}', error message: {}", Strings.mask(email),
          exception.getMessage());
      throw new IdentityProviderUnavailableException("External Service Failure");
    }
  }

  /**
   * Method to create standard user in identity provider.
   *
   * @param user object containing user details.
   * @return {@link User} object with identityProviderId injected.
   * @throws IllegalArgumentException when {@link User#isOwner()} is true
   * @throws IdentityProviderUnavailableException when IdentityProvider fails
   */
  public User createStandardUser(User user) {
    Preconditions.checkNotNull(user.getIdentityProviderUserId(),
        "identityProviderUserId cannot be null");
    Preconditions.checkArgument(!user.isOwner(), "User can't be Administrator");
    Preconditions.checkArgument(StringUtils.hasText(user.getName()), "Standard user must have "
        + "a non-empty name");
    try {
      cognitoClient.adminCreateUser(
          baseAdminCreateUserBuilder(user.getIdentityProviderUserId(), user.getEmail())
              .messageAction(MessageActionType.SUPPRESS)
              .userAttributes(
                  AttributeType.builder().name(NAME_ATTRIBUTE).value(user.getName()).build(),
                  AttributeType.builder().name(EMAIL_ATTRIBUTE).value(user.getEmail()).build(),
                  AttributeType.builder().name(IDENTITY_PROVIDER_ID_ATTRIBUTE).value(
                      user.getIdentityProviderUserId().toString()).build()
              )
              .build()
      );
      return user;
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Builder method used to prepare request params for user creation in identity provider.
   *
   * @param identityProviderId Our custom external identifier.
   * @param email used to create user in Identity Provider.
   * @return {@link AdminCreateUserRequest.Builder} used to create AdminCreateUserRequest which
   *     contains common attributes.
   */
  private AdminCreateUserRequest.Builder baseAdminCreateUserBuilder(
      UUID identityProviderId, String email) {
    return AdminCreateUserRequest.builder()
        .username(email)
        .userAttributes(
            AttributeType.builder()
                .name(EMAIL_ATTRIBUTE)
                .value(email)
                .build(),
            AttributeType.builder()
                .name(IDENTITY_PROVIDER_ID_ATTRIBUTE)
                .value(identityProviderId.toString())
                .build()
        )
        .userPoolId(userPoolId);
  }

  /**
   * Method which gets Identity Provider user ID from returned user object.
   *
   * @param userType User details returned by the identity provider client.
   * @return Cognito User ID based on AWS response.
   * @throws IdentityProviderUnavailableException when {@code attributeName} attribute is not
   *     found in attributes list.
   */
  private String getCognitoUserAttribute(UserType userType, String attributeName) {
    Optional<AttributeType> retrievedAttribute = userType.attributes().stream()
        .filter(attribute -> attribute.name().equals(attributeName))
        .findFirst();

    if (!retrievedAttribute.isPresent()) {
      log.error("Cannot find '{}' property in Identity Service response", attributeName);
      throw new IdentityProviderUnavailableException();
    }

    return retrievedAttribute.get().value();
  }

  /**
   * Method which verifies user's email in Identity Provider.
   *
   * @param user object containing user details.
   */
  public void verifyEmail(User user) {
    sendVerifyEmailRequest(user.getEmail());
  }

  /**
   * Method which verifies user's email in Identity Provider.
   *
   * @param user object containing user details.
   */
  public void verifyEmail(UserEntity user) {
    sendVerifyEmailRequest(user.getEmail());
  }

  /**
   * Method which verifies user's email in Identity Provider.
   *
   * @param email object containing user details.
   */
  private void sendVerifyEmailRequest(String email) {
    try {
      AdminUpdateUserAttributesRequest updateUserAttributesRequest =
          buildVerifyEmailRequest(email);

      cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Method which builds request to verify users email in the identity provider.
   */
  private AdminUpdateUserAttributesRequest buildVerifyEmailRequest(String email) {
    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(email)
        .userAttributes(AttributeType.builder()
            .name("email_verified")
            .value("true")
            .build())
        .build();
  }

  /**
   * Method makes a call to AWS Cognito in order to authenticate user.
   *
   * @param email User's email
   * @param password User's password
   * @return {@link UUID} which is an external unique user identifier.
   */
  public UUID loginUser(String email, String password) {
    String maskedEmail = Strings.mask(email);
    try {
      log.info("Trying to log in user '{}'", maskedEmail);
      AdminInitiateAuthRequest loginRequest = loginRequest(authParamsFor(email, password));
      AdminInitiateAuthResponse response = cognitoClient
          .adminInitiateAuth(loginRequest);
      verifyNullChallengeNameIn(response);
      log.info("User '{}' logged in successfully, response: {}", maskedEmail, response);
      User user = getUser(email);
      if (!user.isEmailVerified()) {
        log.info("Error while logging in user '{}' - the email is not confirmed", maskedEmail);
        throw new EmailNotConfirmedException();
      }
      return user.getIdentityProviderUserId();
    } catch (NotAuthorizedException exception) {
      logAwsExceptionDetails(maskedEmail, exception);
      throw exception;
    } catch (UserNotFoundException exception) {
      logAwsExceptionDetails(maskedEmail, exception);
      throw new InvalidCredentialsException("Invalid credentials");
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(maskedEmail, exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Updates the user's password in Identity provider.
   *
   * @param email User's email
   * @param password User's password
   * @throws IdentityProviderUnavailableException if IdentityProvider fails.
   */
  public void setUserPassword(String email, String password) {
    try {
      cognitoClient.adminSetUserPassword(buildAdminSetUserPasswordRequest(email, password));
      updatePreviousPasswordsAndTimestampAttribute(email, password);
    } catch (InvalidPasswordException | InvalidParameterException exception) {
      logAwsExceptionDetails(exception);
      throw new PasswordInvalidException("Parameters cannot be processed");
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Clears last password for user.
   *
   * @param email User's email
   */
  public void clearPreviousPasswordsForUser(String email) {
    updatePreviousPasswordsAndTimestampAttribute(email, "");
  }

  /**
   * Updates previous passwords and last update timestamp.
   */
  private void updatePreviousPasswordsAndTimestampAttribute(String email, String password) {
    String previousPasswordsRaw = getPreviousPasswords(email);
    cognitoClient.adminUpdateUserAttributes(
        preparePreviousPasswordsAndTimestampUpdateRequest(email, password, previousPasswordsRaw));
  }

  /**
   * Retrieves previous passwords raw value for an user.
   */
  private String getPreviousPasswords(String email) {
    AdminGetUserResponse response = cognitoClient.adminGetUser(buildAdminGetUserRequest(email));
    Optional<AttributeType> previousPasswordsAttribute = AdminGetUserResponseToUserConverter
        .getOptionalAttributeFromResponse(response, PREVIOUS_PASSWORDS_ATTRIBUTE);
    String previousPasswordsRaw = previousPasswordsAttribute
        .map(attributeType -> attributeType.value()).orElse("");
    return previousPasswordsRaw;
  }

  /**
   * Retrieves previous passwords value for an user.
   */
  public List<String> previousPasswordsForUser(String email) {
    String previousPasswordsRaw = getPreviousPasswords(email);
    return Arrays
        .stream(previousPasswordsRaw.split(PREVIOUS_PASSWORDS_DELIMITER))
        .collect(Collectors.toCollection(Lists::newArrayList));
  }

  /**
   * Prepares {@link AdminUpdateUserAttributesRequest} object with previous passwords and last
   * update timestamp.
   */
  private AdminUpdateUserAttributesRequest preparePreviousPasswordsAndTimestampUpdateRequest(
      String email, String password, String previousPasswordsRaw) {
    AdminUpdateUserAttributesRequest updateUserAttributesRequest = AdminUpdateUserAttributesRequest
        .builder()
        .userPoolId(userPoolId)
        .username(email)
        .userAttributes(
            AttributeType.builder()
                .name(PREVIOUS_PASSWORDS_ATTRIBUTE)
                .value(updatePreviousPasswords(previousPasswordsRaw, password))
                .build(),
            AttributeType.builder()
                .name(PASSWORD_LAST_UPDATE_TIMESTAMP_ATTRIBUTE)
                .value(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build()
        )
        .build();
    return updateUserAttributesRequest;
  }

  /**
   * Hashes {@code newPassword} and adds it at the beginning of joined stack of previous password
   * hashes while keeping them at {@code MAX_PREVIOUS_PASSWORDS} limit.
   */
  private String updatePreviousPasswords(String previousPasswordsRaw, String newPassword) {
    Deque<String> previousPasswords = Arrays
        .stream(previousPasswordsRaw.split(PREVIOUS_PASSWORDS_DELIMITER))
        .collect(Collectors.toCollection(ArrayDeque::new));
    if (previousPasswords.size() >= MAX_PREVIOUS_PASSWORDS) {
      previousPasswords.pollLast();
    }
    previousPasswords.addFirst(Sha2Hasher.sha256Hash(newPassword));
    return previousPasswords.stream().collect(Collectors.joining(PREVIOUS_PASSWORDS_DELIMITER));
  }

  /**
   * Updates the user's name in Identity provider.
   *
   * @param email User's email
   * @param name User's name
   * @throws IdentityProviderUnavailableException if IdentityProvider fails.
   */
  public void setUserName(String email, String name) {
    try {
      cognitoClient.adminUpdateUserAttributes(buildAdminSetUserName(email, name));
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Method which increase failed-logins attribute by one.
   *
   * @param email {@link String} containing email.
   */
  public void increaseFailedLoginsByOne(String email) {
    try {
      AdminUpdateUserAttributesRequest updateUserAttributesRequest = updateFailedLoginsAttribute(
          getUserAsUserEntity(email),
          Integer.toString(getCurrentFailedLogins(email) + 1));

      cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Method which set lockout-time attribute.
   *
   * @param email {@link String} containing email.
   */
  public void setLockoutTime(String email) {
    try {
      AdminUpdateUserAttributesRequest updateUserAttributesRequest = updateLockoutTimeAttribute(
          getUserAsUserEntity(email));

      cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Method which set failed-logins attribute and set lockout-time attribute to null.
   *
   * @param email {@link String} containing email.
   */
  public void resetFailedLoginsAndLockoutTime(String email) {
    try {
      UserEntity user = getUserAsUserEntity(email);

      AdminUpdateUserAttributesRequest updateUserAttributesRequest =
          updateFailedLoginsAndLockoutTimeAttributes(user);

      cognitoClient.adminUpdateUserAttributes(updateUserAttributesRequest);
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Method which get current failed-logins attribute.
   *
   * @param email {@link String} containing email.
   */
  public int getCurrentFailedLogins(String email) {
    UserEntity user = getUserAsUserEntity(email);
    return user.getFailedLogins();
  }

  /**
   * Method which get current lockout-time attribute.
   *
   * @param email {@link String} containing email.
   */
  public Optional<LocalDateTime> getCurrentLockoutTime(String email) {
    UserEntity user = getUserAsUserEntity(email);
    return user.getLockoutTime();
  }

  /**
   * Gets timestamp of when was the last password update (or user creation if not yet known).
   *
   * @param email email {@link String} containing email.
   * @return timestamp of when was the last password update (or user creation if not yet known).
   */
  public LocalDateTime getPasswordUpdateTimestamp(String email) {
    return getUserAsUserEntity(email).getPasswordUpdateTimestamp();
  }

  private AdminUpdateUserAttributesRequest updateFailedLoginsAndLockoutTimeAttributes(
      UserEntity user) {
    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmail())
        .userAttributes(
            AttributeType.builder()
                .name(FAILED_LOGINS_ATTRIBUTE)
                .value(Integer.toString(0))
                .build(),
            AttributeType.builder()
                .name(LOCKOUT_TIME_ATTRIBUTE)
                .value("")
                .build()
        )
        .build();
  }

  private AdminUpdateUserAttributesRequest updateLockoutTimeAttribute(UserEntity user) {
    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmail())
        .userAttributes(
            AttributeType.builder()
                .name(LOCKOUT_TIME_ATTRIBUTE)
                .value(LocalDateTime.now().format(FORMATTER))
                .build()
        )
        .build();
  }

  private AdminUpdateUserAttributesRequest updateFailedLoginsAttribute(
      UserEntity user, String failedLogins) {
    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(user.getEmail())
        .userAttributes(AttributeType.builder()
            .name(FAILED_LOGINS_ATTRIBUTE)
            .value(failedLogins)
            .build())
        .build();
  }

  /**
   * Method to prepare request params for setting password for user in identity provider.
   *
   * @param email User's email
   * @param password User's password
   * @return {@link AdminSetUserPasswordRequest} object which contains data to be send to cognito.
   */
  private AdminSetUserPasswordRequest buildAdminSetUserPasswordRequest(String email,
      String password) {
    return AdminSetUserPasswordRequest.builder()
        .userPoolId(userPoolId)
        .password(password)
        .username(email)
        .permanent(true)
        .build();
  }

  /**
   * Method to prepare request params for setting name for user in identity provider.
   *
   * @param email User's email
   * @param name User's name
   * @return {@link AdminSetUserSettingsRequest} object which contains data to be send to cognito.
   */
  private AdminUpdateUserAttributesRequest buildAdminSetUserName(String email,
      String name) {
    return AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(email)
        .userAttributes(AttributeType.builder().name(NAME_ATTRIBUTE).value(name).build())
        .build();
  }

  /**
   * Logs details of the {@code exception}.
   */
  private void logAwsExceptionDetails(CognitoIdentityProviderException exception) {
    AwsErrorDetails details = exception.awsErrorDetails();
    log.error("Error code: {}, Error message: {}", details.errorCode(), details.errorMessage());
  }

  /**
   * Logs details of the {@code exception} alongside the email address.
   */
  private void logAwsExceptionDetails(String maskedEmail,
      CognitoIdentityProviderException exception) {
    AwsErrorDetails details = exception.awsErrorDetails();
    log.warn("Unable to log in user: '{}', error code: {}, error message: {}", maskedEmail,
        details.errorCode(), details.errorMessage());
  }

  /**
   * Helper method which prepares auth params.
   */
  private Map<String, String> authParamsFor(String username, String password) {
    return ImmutableMap.of(
        "USERNAME", username,
        "PASSWORD", password,
        "SECRET_HASH", calculateSecretHash(appClientId, appClientSecret, username)
    );
  }

  /**
   * Helper method to build {@link AdminInitiateAuthRequest}.
   */
  private AdminInitiateAuthRequest loginRequest(Map<String, String> authParams) {
    return AdminInitiateAuthRequest.builder()
        .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
        .authParameters(authParams)
        .clientId(appClientId)
        .userPoolId(userPoolId)
        .build();
  }

  /**
   * Creates a new user with {@code newEmail} as username based on the existing one identified by
   * {@code identityProviderIdOfUserToBeCloned}. The new user is not verified and does not contain
   * any password-related attributes.
   */
  public void cloneUserAndSetEmailTo(UUID identityProviderIdOfUserToBeCloned,
      UUID newIdentityProviderUserId, String newEmail) {
    // assertion: uniqueness of 'newEmail' has already been checked, other arguments are non null

    UserEntity toClone = getUserAsUserEntityByExternalId(identityProviderIdOfUserToBeCloned);
    try {
      cognitoClient.adminCreateUser(
          baseAdminCreateUserBuilder(newIdentityProviderUserId, newEmail)
              .messageAction(MessageActionType.SUPPRESS)
              .userAttributes(createAttributesForClonedUser(newIdentityProviderUserId, newEmail,
                  toClone.getName()))
              .build()
      );
    } catch (CognitoIdentityProviderException exception) {
      logAwsExceptionDetails(exception);
      throw new IdentityProviderUnavailableException();
    }
  }

  /**
   * Creates a {@code List<AttributeType>} based on an existing user - this will be the attributes
   * of a newly created user in cognito.
   */
  private List<AttributeType> createAttributesForClonedUser(UUID newIdentityProviderUserId,
      String newEmail, String existingName) {
    Builder<AttributeType> attributesBuilder = ImmutableList.builder();
    if (StringUtils.hasText(existingName)) {
      attributesBuilder.add(
          AttributeType.builder().name(NAME_ATTRIBUTE).value(existingName).build()
      );
    }
    attributesBuilder.add(
        AttributeType.builder().name(EMAIL_ATTRIBUTE).value(newEmail).build(),
        AttributeType.builder().name(IDENTITY_PROVIDER_ID_ATTRIBUTE).value(
            newIdentityProviderUserId.toString()).build()
    );
    return attributesBuilder.build();
  }
}
