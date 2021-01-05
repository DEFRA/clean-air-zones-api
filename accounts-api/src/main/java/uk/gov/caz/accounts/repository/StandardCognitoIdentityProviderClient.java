package uk.gov.caz.accounts.repository;

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
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;

public class StandardCognitoIdentityProviderClient implements
    uk.gov.caz.accounts.repository.CognitoIdentityProviderClient {

  private final CognitoIdentityProviderClient cognitoClient;

  public StandardCognitoIdentityProviderClient(CognitoClientProvider cognitoClientProvider) {
    cognitoClient = cognitoClientProvider.getNewCognitoClient();
  }

  @Override
  public ListUsersResponse listUsers(ListUsersRequest listUsersRequest) {
    return cognitoClient.listUsers(listUsersRequest);
  }

  @Override
  public AdminGetUserResponse adminGetUser(AdminGetUserRequest adminGetUserRequest) {
    return cognitoClient.adminGetUser(adminGetUserRequest);
  }

  @Override
  public AdminCreateUserResponse adminCreateUser(AdminCreateUserRequest adminCreateUserRequest) {
    return cognitoClient.adminCreateUser(adminCreateUserRequest);
  }

  @Override
  public AdminDeleteUserResponse adminDeleteUser(AdminDeleteUserRequest adminDeleteUserRequest) {
    return cognitoClient.adminDeleteUser(adminDeleteUserRequest);
  }

  @Override
  public AdminUpdateUserAttributesResponse adminUpdateUserAttributes(
      AdminUpdateUserAttributesRequest adminUpdateUserAttributesRequest) {
    return cognitoClient.adminUpdateUserAttributes(adminUpdateUserAttributesRequest);
  }

  @Override
  public AdminInitiateAuthResponse adminInitiateAuth(
      AdminInitiateAuthRequest adminInitiateAuthRequest) {
    return cognitoClient.adminInitiateAuth(adminInitiateAuthRequest);
  }

  @Override
  public AdminSetUserPasswordResponse adminSetUserPassword(
      AdminSetUserPasswordRequest adminSetUserPasswordRequest) {
    return cognitoClient.adminSetUserPassword(adminSetUserPasswordRequest);
  }

  @Override
  public AdminListGroupsForUserResponse adminListGroupsForUser(
      AdminListGroupsForUserRequest adminListGroupsForUserRequest) {
    return cognitoClient.adminListGroupsForUser(adminListGroupsForUserRequest);
  }

}
