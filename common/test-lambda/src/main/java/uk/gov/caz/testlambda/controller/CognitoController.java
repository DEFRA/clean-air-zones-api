package uk.gov.caz.testlambda.controller;

import static uk.gov.caz.testlambda.util.SecretHashCalculator.calculateSecretHash;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import uk.gov.caz.testlambda.dto.LoginUserDto;
import uk.gov.caz.testlambda.dto.NewCognitoUserDto;
import uk.gov.caz.testlambda.dto.VerifyEmailDto;

@RestController
@AllArgsConstructor
@Slf4j
public class CognitoController implements CognitoControllerApiSpec {

  public static final String PATH = "/cognito/users";

  private final CognitoIdentityProviderClient cognitoClient;

  @Override
  public ResponseEntity<String> createUser(NewCognitoUserDto newUserData) {
    createUserWithTemporaryPassword(newUserData);

    setUserProvidedPasswordAndConfirmAccount(newUserData);

    return ResponseEntity.ok("OK");
  }

  @Override
  public ResponseEntity<String> loginUser(LoginUserDto loginUserData) {
    final Map<String, String> authParams = prepareLoginParams(loginUserData);
    AdminInitiateAuthRequest loginRequest = prepareLoginRequest(loginUserData, authParams);

    try {
      AdminInitiateAuthResponse adminInitiateAuthResponse = cognitoClient
          .adminInitiateAuth(loginRequest);
      logSuccessfullResponse(adminInitiateAuthResponse);

      AdminGetUserRequest adminGetUserRequest = prepareGetUserRequest(loginUserData);
      AdminGetUserResponse adminGetUserResponse = cognitoClient.adminGetUser(adminGetUserRequest);
      log.info("User details: {}", adminGetUserResponse);
    } catch (NotAuthorizedException notAuthorizedException) {
      log.error("Unable to login user: {}", notAuthorizedException.getMessage());
    }

    return ResponseEntity.ok("OK");
  }

  @Override
  public ResponseEntity<String> verifyEmail(VerifyEmailDto verifyEmailData) {
    AdminUpdateUserAttributesRequest updateUserAttributesRequest = prepareUpdateUserAttributesRequest(
        verifyEmailData);

    AdminUpdateUserAttributesResponse adminUpdateUserAttributesResponse = cognitoClient
        .adminUpdateUserAttributes(updateUserAttributesRequest);
    log.info("Updated user attributes: {}", adminUpdateUserAttributesResponse);

    return ResponseEntity.ok("OK");
  }

  @Override
  public ResponseEntity<String> getUser(String username, String userPoolId) {
    AdminGetUserRequest adminGetUserRequest = prepareGetUserRequest(username, userPoolId);
    AdminGetUserResponse adminGetUserResponse = cognitoClient.adminGetUser(adminGetUserRequest);
    log.info("User details: {}", adminGetUserResponse);
    log.info("Username: {}", adminGetUserResponse.username());

    return ResponseEntity.ok("OK");
  }

  private AdminInitiateAuthRequest prepareLoginRequest(LoginUserDto loginUserData,
      Map<String, String> authParams) {
    return AdminInitiateAuthRequest.builder()
        .userPoolId(loginUserData.getUserPoolId())
        .clientId(loginUserData.getClientId())
        .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
        .authParameters(authParams)
        .build();
  }

  private void createUserWithTemporaryPassword(NewCognitoUserDto newUserData) {
    AdminCreateUserRequest createUserRequest = prepareCreateUserRequest(newUserData);
    AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);
    log.info("Created user: {}", createUserResponse.user().toString());
  }

  private AdminCreateUserRequest prepareCreateUserRequest(NewCognitoUserDto newUserData) {
    return AdminCreateUserRequest.builder()
        .userPoolId(newUserData.getUserPoolId())
        .username(newUserData.getUserName())
        .temporaryPassword("temppassword")
        .userAttributes(prepareEmailAttribute(newUserData))
        .messageAction(MessageActionType.SUPPRESS) // do not send email to the user
        .build();
  }

  private AttributeType prepareEmailAttribute(NewCognitoUserDto newUserData) {
    return AttributeType.builder()
        .name("email")
        .value(newUserData.getEmail())
        .build();
  }

  private void setUserProvidedPasswordAndConfirmAccount(NewCognitoUserDto newUserData) {
    AdminSetUserPasswordRequest setUserPasswordRequest = prepareSetPasswordRequest(newUserData);
    AdminSetUserPasswordResponse adminSetUserPasswordResponse = cognitoClient
        .adminSetUserPassword(setUserPasswordRequest);
    log.info("Set password for user: {}", adminSetUserPasswordResponse);
  }

  private AdminSetUserPasswordRequest prepareSetPasswordRequest(NewCognitoUserDto newUserData) {
    return AdminSetUserPasswordRequest.builder()
        .userPoolId(newUserData.getUserPoolId())
        .username(newUserData.getUserName())
        .password(newUserData.getPassword())
        .permanent(true)
        .build();
  }

  @NotNull
  private Map<String, String> prepareLoginParams(LoginUserDto loginUserData) {
    final Map<String, String> authParams = new HashMap<>();
    authParams.put("USERNAME", loginUserData.getUserName());
    authParams.put("PASSWORD", loginUserData.getPassword());
    authParams.put("SECRET_HASH",
        calculateSecretHash(
            loginUserData.getClientId(),
            loginUserData.getClientSecret(),
            loginUserData.getUserName()));
    return authParams;
  }

  private void logSuccessfullResponse(AdminInitiateAuthResponse adminInitiateAuthResponse) {
    log.info("Logged user: {}. Access token: {}, expires in: {}",
        adminInitiateAuthResponse,
        adminInitiateAuthResponse.authenticationResult().accessToken(),
        adminInitiateAuthResponse.authenticationResult().expiresIn());
  }

  private AdminGetUserRequest prepareGetUserRequest(LoginUserDto loginUserData) {
    return AdminGetUserRequest.builder()
        .userPoolId(loginUserData.getUserPoolId())
        .username(loginUserData.getUserName())
        .build();
  }

  private AdminGetUserRequest prepareGetUserRequest(String username, String userPoolId) {
    return AdminGetUserRequest.builder()
        .userPoolId(userPoolId)
        .username(username)
        .build();
  }

  private AdminUpdateUserAttributesRequest prepareUpdateUserAttributesRequest(
      VerifyEmailDto verifyEmailData) {
    return AdminUpdateUserAttributesRequest
        .builder()
        .userPoolId(verifyEmailData.getUserPoolId())
        .username(verifyEmailData.getUserName())
        .userAttributes(AttributeType.builder()
            .name("email_verified")
            .value("true")
            .build())
        .build();
  }
}
