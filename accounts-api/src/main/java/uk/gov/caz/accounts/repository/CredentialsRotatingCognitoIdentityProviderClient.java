package uk.gov.caz.accounts.repository;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;

/**
 * Wrapper over CognitoIdentityProviderClient that provides credentials rotating capabilities -
 * but only if needed.
 */
@Slf4j
public class CredentialsRotatingCognitoIdentityProviderClient implements
    uk.gov.caz.accounts.repository.CognitoIdentityProviderClient {

  private final CognitoClientProvider cognitoClientProvider;
  private CognitoIdentityProviderClient cognitoIdentityProviderClient;

  public CredentialsRotatingCognitoIdentityProviderClient(
      CognitoClientProvider cognitoClientProvider) {
    this.cognitoClientProvider = cognitoClientProvider;
    this.cognitoIdentityProviderClient = cognitoClientProvider.getNewCognitoClient();
  }

  @Override
  public ListUsersResponse listUsers(ListUsersRequest listUsersRequest) {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.listUsers(
        listUsersRequest
    ));
  }

  @Override
  public AdminGetUserResponse adminGetUser(
      AdminGetUserRequest adminGetUserRequest)  {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminGetUser(
        adminGetUserRequest
    ));
  }

  @Override
  public AdminCreateUserResponse adminCreateUser(AdminCreateUserRequest adminCreateUserRequest)
      throws AwsServiceException, SdkClientException {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminCreateUser(
        adminCreateUserRequest
    ));
  }

  @Override
  public AdminDeleteUserResponse adminDeleteUser(AdminDeleteUserRequest adminDeleteUserRequest)
      throws AwsServiceException, SdkClientException {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminDeleteUser(
        adminDeleteUserRequest
    ));
  }

  @Override
  public AdminUpdateUserAttributesResponse adminUpdateUserAttributes(
      AdminUpdateUserAttributesRequest adminUpdateUserAttributesRequest)
      throws AwsServiceException, SdkClientException {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminUpdateUserAttributes(
        adminUpdateUserAttributesRequest
    ));
  }

  @Override
  public AdminInitiateAuthResponse adminInitiateAuth(
      AdminInitiateAuthRequest adminInitiateAuthRequest) {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminInitiateAuth(
        adminInitiateAuthRequest
    ));
  }

  @Override
  public AdminSetUserPasswordResponse adminSetUserPassword(
      AdminSetUserPasswordRequest adminSetUserPasswordRequest) {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminSetUserPassword(
        adminSetUserPasswordRequest
    ));
  }

  @Override
  public AdminListGroupsForUserResponse adminListGroupsForUser(
      AdminListGroupsForUserRequest adminListGroupsForUserRequest) {
    return credentialsRotatingCall(() -> cognitoIdentityProviderClient.adminListGroupsForUser(
        adminListGroupsForUserRequest
    ));
  }

  private <A> A credentialsRotatingCall(Callable<A> callable) {
    try {
      return callable.call();
    } catch (CognitoIdentityProviderException e) {
      log.info("Trying to rotate Cognito keys. Exception {} with message {}",
          e.getClass().toString(),
          e.getMessage()
      );
      this.cognitoIdentityProviderClient = cognitoClientProvider.getNewCognitoClient();
      return callable.call();
    }
  }

  @FunctionalInterface
  private interface Callable<A> {
    A call();
  }
}