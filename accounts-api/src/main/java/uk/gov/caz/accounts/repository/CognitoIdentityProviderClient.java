package uk.gov.caz.accounts.repository;

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

public interface CognitoIdentityProviderClient {

  ListUsersResponse listUsers(ListUsersRequest listUsersRequest);

  AdminGetUserResponse adminGetUser(AdminGetUserRequest adminGetUserRequest);

  AdminCreateUserResponse adminCreateUser(AdminCreateUserRequest adminCreateUserRequest);

  AdminDeleteUserResponse adminDeleteUser(AdminDeleteUserRequest adminDeleteUserRequest);

  AdminUpdateUserAttributesResponse adminUpdateUserAttributes(
      AdminUpdateUserAttributesRequest adminUpdateUserAttributesRequest);

  AdminInitiateAuthResponse adminInitiateAuth(AdminInitiateAuthRequest adminInitiateAuthRequest);

  AdminSetUserPasswordResponse adminSetUserPassword(
      AdminSetUserPasswordRequest adminSetUserPasswordRequest);

  AdminListGroupsForUserResponse adminListGroupsForUser(
      AdminListGroupsForUserRequest adminListGroupsForUserRequest);
}
