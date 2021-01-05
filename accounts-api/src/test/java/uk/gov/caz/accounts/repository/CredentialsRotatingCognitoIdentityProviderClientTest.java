package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@ExtendWith(MockitoExtension.class)
class CredentialsRotatingCognitoIdentityProviderClientTest {

  @Mock
  private CognitoIdentityProviderClient cognitoClientProviderClient;

  private CredentialsRotatingCognitoIdentityProviderClient identityProviderClient;

  @BeforeEach
  public void setup() throws Exception {
    SecretServiceBasedCognitoClientProvider cognitoClientProvider = mock(
        SecretServiceBasedCognitoClientProvider.class);
    when(cognitoClientProvider.getNewCognitoClient()).thenReturn(cognitoClientProviderClient);

    identityProviderClient = new CredentialsRotatingCognitoIdentityProviderClient(
        cognitoClientProvider
    );
  }

  @Test
  public void shouldNotRotateCredentialsForSuccessfulFirstCall() {
    //given
    ListUsersRequest listUsersRequest = ListUsersRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();
    ListUsersResponse listUsersResponse = ListUsersResponse.builder()
        .users(UserType.builder().enabled(false).build()).build();

    when(identityProviderClient.listUsers(listUsersRequest)).thenReturn(listUsersResponse);

    //when
    ListUsersResponse usersResponse = identityProviderClient.listUsers(listUsersRequest);

    //then
    assertThat(usersResponse).isEqualTo(listUsersResponse);
  }

  @Test
  public void shouldRotateCredentialsForCognitoIdentityProviderException() {
    //given
    ListUsersRequest listUsersRequest = ListUsersRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();
    ListUsersResponse listUsersResponse = ListUsersResponse.builder()
        .users(UserType.builder().enabled(false).build()).build();

    when(identityProviderClient.listUsers(listUsersRequest))
        .thenThrow(CognitoIdentityProviderException.builder().build())
        .thenReturn(listUsersResponse);

    //when
    ListUsersResponse usersResponse = identityProviderClient.listUsers(listUsersRequest);

    //then
    assertThat(usersResponse).isEqualTo(listUsersResponse);
    verify(cognitoClientProviderClient, times(2))
        .listUsers(listUsersRequest);
  }

  @Test
  public void shouldRethrowExceptionIfRotatedCredentialsFailed() {
    //given
    ListUsersRequest listUsersRequest = ListUsersRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();

    when(identityProviderClient.listUsers(listUsersRequest))
        .thenThrow(CognitoIdentityProviderException.builder().build());

    //then
    assertThrows(CognitoIdentityProviderException.class, () -> {
      identityProviderClient.listUsers(listUsersRequest);
    });
  }

  @Test
  public void shouldRethrowRuntimeExceptionAndNotRotateCredentials() {
    //given
    ListUsersRequest listUsersRequest = ListUsersRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();
    when(identityProviderClient.listUsers(listUsersRequest))
        .thenThrow(new RuntimeException());

    //then
    assertThrows(RuntimeException.class, () -> {
      identityProviderClient.listUsers(listUsersRequest);
    });
    verify(cognitoClientProviderClient, times(1))
        .listUsers(listUsersRequest);
  }


  @Test
  public void shouldCallAdminGetUserWithProvidedRequest() {
    //given
    AdminGetUserRequest adminGetUserRequest = AdminGetUserRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();

    AdminGetUserResponse mockedAdminGetUserResponse = AdminGetUserResponse.builder()
        .enabled(false)
        .build();

    when(identityProviderClient.adminGetUser(adminGetUserRequest))
        .thenReturn(mockedAdminGetUserResponse);

    //when
    AdminGetUserResponse adminGetUserResponse =
        identityProviderClient.adminGetUser(adminGetUserRequest);

    //then
    assertThat(mockedAdminGetUserResponse).isEqualTo(adminGetUserResponse);
  }

  @Test
  public void shouldCalladminCreateUserWithProvidedRequest() {
    //given
    AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();

    AdminCreateUserResponse mockedAdminGetUserResponse = AdminCreateUserResponse.builder()
        .user(UserType.builder().enabled(false).build())
        .build();

    when(identityProviderClient.adminCreateUser(adminCreateUserRequest))
        .thenReturn(mockedAdminGetUserResponse);

    //when
    AdminCreateUserResponse adminCreateUserResponse = identityProviderClient
        .adminCreateUser(adminCreateUserRequest);

    //then
    assertThat(mockedAdminGetUserResponse).isEqualTo(adminCreateUserResponse);
  }

  @Test
  public void shouldCalladminDeleteUserWithProvidedRequest() {
    //given
    AdminDeleteUserRequest mockAdminDeleteUserRequest = AdminDeleteUserRequest.builder()
        .userPoolId(UUID.randomUUID().toString())
        .build();

    AdminDeleteUserResponse adminDeleteUserResponse = AdminDeleteUserResponse.builder()
        .build();

    when(identityProviderClient.adminDeleteUser(mockAdminDeleteUserRequest))
        .thenReturn(adminDeleteUserResponse);

    //when
    AdminDeleteUserResponse adminCreateUserResponse = identityProviderClient
        .adminDeleteUser(mockAdminDeleteUserRequest);

    //then
    assertThat(adminDeleteUserResponse).isEqualTo(adminCreateUserResponse);
  }

  @Test
  public void shouldCalladminUpdateUserAttributesWithProvidedRequest() {
    //given
    AdminUpdateUserAttributesRequest adminUpdateUserAttributesRequest =
        AdminUpdateUserAttributesRequest.builder().userPoolId(UUID.randomUUID().toString())
        .build();

    AdminUpdateUserAttributesResponse adminUpdateUserAttributesResponse =
        AdminUpdateUserAttributesResponse.builder()
        .build();

    when(identityProviderClient.adminUpdateUserAttributes(adminUpdateUserAttributesRequest))
        .thenReturn(adminUpdateUserAttributesResponse);

    //when
    AdminUpdateUserAttributesResponse adminCreateUserResponse = identityProviderClient
        .adminUpdateUserAttributes(adminUpdateUserAttributesRequest);

    //then
    assertThat(adminUpdateUserAttributesResponse).isEqualTo(adminCreateUserResponse);
  }

  @Test
  public void shouldCalladminInitiateAuthWithProvidedRequest() {
    //given
    AdminInitiateAuthRequest adminInitiateAuthRequest =
        AdminInitiateAuthRequest.builder().userPoolId(UUID.randomUUID().toString())
            .build();

    AdminInitiateAuthResponse adminInitiateAuthResponse =
        AdminInitiateAuthResponse.builder()
            .build();

    when(identityProviderClient.adminInitiateAuth(adminInitiateAuthRequest))
        .thenReturn(adminInitiateAuthResponse);

    //when
    AdminInitiateAuthResponse adminCreateUserResponse = identityProviderClient
        .adminInitiateAuth(adminInitiateAuthRequest);

    //then
    assertThat(adminInitiateAuthResponse).isEqualTo(adminCreateUserResponse);
  }

  @Test
  public void shouldCallaadminSetUserPasswordWithProvidedRequest() {
    //given
    AdminSetUserPasswordRequest adminInitiateAuthRequest =
        AdminSetUserPasswordRequest.builder().userPoolId(UUID.randomUUID().toString())
            .build();

    AdminSetUserPasswordResponse mockedAdminSetUserPasswordResponse =
        AdminSetUserPasswordResponse.builder()
            .build();

    when(identityProviderClient.adminSetUserPassword(adminInitiateAuthRequest))
        .thenReturn(mockedAdminSetUserPasswordResponse);

    //when
    AdminSetUserPasswordResponse adminSetUserPasswordResponse = identityProviderClient
        .adminSetUserPassword(adminInitiateAuthRequest);

    //then
    assertThat(mockedAdminSetUserPasswordResponse).isEqualTo(adminSetUserPasswordResponse);
  }

  @Test
  public void shouldCallaadminUpdateUserAttributesWithProvidedRequest() {
    //given
    AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
        .userPoolId(UUID.randomUUID().toString()).build();

    AdminUpdateUserAttributesResponse expectedResponse =
        AdminUpdateUserAttributesResponse.builder()
            .build();

    when(identityProviderClient.adminUpdateUserAttributes(request))
        .thenReturn(expectedResponse);

    //when
    AdminUpdateUserAttributesResponse realResponse = identityProviderClient
        .adminUpdateUserAttributes(request);

    //then
    assertThat(realResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void shouldCallAdminListGroupsForUserWithProvidedRequest() {
    //given
    AdminListGroupsForUserRequest request = AdminListGroupsForUserRequest.builder()
        .username("username")
        .userPoolId(UUID.randomUUID().toString()).build();

    AdminListGroupsForUserResponse expectedResponse =
        AdminListGroupsForUserResponse.builder()
            .build();

    when(identityProviderClient.adminListGroupsForUser(request))
        .thenReturn(expectedResponse);

    //when
    AdminListGroupsForUserResponse realResponse = identityProviderClient
        .adminListGroupsForUser(request);

    //then
    assertThat(realResponse).isEqualTo(expectedResponse);
  }
}