package uk.gov.caz.accounts.repository;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
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

@ExtendWith(MockitoExtension.class)
class IdentityProviderTest {

  private final String LOCKOUT_TIME = LocalDateTime.now().format(ISO_DATE_TIME);
  private IdentityProvider identityProvider;
  private final String USER_POOL_ID = "sample_pool_id";
  private final String APP_CLIENT_ID = "sample_client_id";
  private final String APP_CLIENT_SECRET = "sample_client_secret";
  private final UUID ANY_IDENTITY_PROVIDER_ID = UUID.randomUUID();
  private final UUID ANY_SUB_ID = UUID.randomUUID();
  private final String ANY_PASSWORD = "password";
  private final String ANY_EMAIL = "example@email.com";

  @Mock
  CredentialsRotatingCognitoIdentityProviderClient cognitoClient;

  @Captor
  ArgumentCaptor<AdminUpdateUserAttributesRequest> updateUserAttributesRequestArgumentCaptor;

  @BeforeEach
  public void initialize() {
    identityProvider = new IdentityProvider(USER_POOL_ID, APP_CLIENT_ID, APP_CLIENT_SECRET,
        cognitoClient);
  }

  @Nested
  class CheckIfUserExists {

    private List<UserType> NON_EMPTY_COLLECTION = Arrays.asList(UserType.builder().build());
    private List<UserType> EMPTY_COLLECTION = new ArrayList();

    private ListUsersResponse nonEmptyListUsersResponse = ListUsersResponse.builder()
        .users(NON_EMPTY_COLLECTION).build();
    private ListUsersResponse emptyListUsersResponse = ListUsersResponse.builder()
        .users(EMPTY_COLLECTION).build();

    private String ANY_EMAIL = "jan@kowalski.com";

    @Test
    void shouldReturnTrueWhenUnderlyingIdentitySourceContainsUser() {
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(nonEmptyListUsersResponse);

      assertThat(identityProvider.checkIfUserExists(ANY_EMAIL)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUnderlyingIdentitySourceDoesNotHaveUser() {
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(emptyListUsersResponse);

      assertThat(identityProvider.checkIfUserExists(ANY_EMAIL)).isFalse();
    }
  }

  @Nested
  class GetEmailByIdentityProviderId {

    private UUID ANY_IDENTITY_PROVIDER_ID = UUID.randomUUID();
    private String ANY_EMAIL = "test@email.com";
    private List<UserType> NON_EMPTY_COLLECTION = Arrays.asList(
        UserType.builder().attributes(
            AttributeType.builder().name("email").value(ANY_EMAIL).build()
        ).build());
    private List<UserType> EMPTY_COLLECTION = new ArrayList();

    private ListUsersResponse nonEmptyListUsersResponse = ListUsersResponse.builder()
        .users(NON_EMPTY_COLLECTION).build();
    private ListUsersResponse emptyListUsersResponse = ListUsersResponse.builder()
        .users(EMPTY_COLLECTION).build();

    @Test
    void shouldReturnUserWhenIdentityProviderUserFound() {
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(nonEmptyListUsersResponse);

      assertThat(identityProvider.getEmailByIdentityProviderId(ANY_IDENTITY_PROVIDER_ID))
          .isEqualTo(ANY_EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenIdentityProviderUserNotFound() {
      // given
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(emptyListUsersResponse);

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.getEmailByIdentityProviderId(ANY_IDENTITY_PROVIDER_ID));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    void shouldThrowExceptionWhenEmailAttributeIsAbsent() {
      // given
      mockInvalidResponseWithoutEmailAttribute();

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.getEmailByIdentityProviderId(ANY_IDENTITY_PROVIDER_ID));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
    }

    private void mockInvalidResponseWithoutEmailAttribute() {
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(ListUsersResponse.builder()
              .users(UserType.builder()
                  .attributes(AttributeType.builder().name("a").value("b").build()).build())
              .build());
    }
  }

  @Nested
  class GetUser {

    private String ANY_EMAIL = "jan@kowalski.com";
    private String ANY_NAME = "any-janek";
    private String ANY_UUID = "e861a148-5957-4549-b8e1-9790c99c11e3";

    private AttributeType emailAttribute = AttributeType.builder().name("email").value(ANY_EMAIL)
        .build();
    private AttributeType subAttribute = AttributeType.builder().name("sub").value(ANY_UUID)
        .build();
    private AttributeType nameAttribute = AttributeType.builder().name("name").value(ANY_NAME)
        .build();
    private AttributeType preferredUsernameAttribute = AttributeType.builder()
        .name("preferred_username")
        .value(ANY_IDENTITY_PROVIDER_ID.toString())
        .build();

    private AdminGetUserResponse adminGetUserResponse = AdminGetUserResponse.builder()
        .username(ANY_UUID)
        .userAttributes(Arrays.asList(emailAttribute, subAttribute, preferredUsernameAttribute,
            nameAttribute))
        .build();

    @Test
    void shouldReturnUserWhenUnderlyingIdentitySourceReturnsUser() {
      when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class)))
          .thenReturn(adminGetUserResponse);

      assertThat(identityProvider.getUser(ANY_EMAIL).getEmail()).isEqualTo(ANY_EMAIL);
      assertThat(identityProvider.getUser(ANY_EMAIL).getName()).isEqualTo(ANY_NAME);
    }
  }

  @Nested
  class CreateStandardUser {

    @Test
    void shouldReturnUserWithIdWhenValidParamsProvided() {
      // given
      String username = "TestName";
      String email = "test@email.com";
      User user = User.builder()
          .identityProviderUserId(ANY_IDENTITY_PROVIDER_ID)
          .name(username)
          .email(email)
          .build();
      AdminCreateUserResponse cognitoUser = successfullyCreatedCognitoUser();
      when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
          .thenReturn(cognitoUser);

      // when
      User userWithCognito = identityProvider.createStandardUser(user);

      //then
      assertThat(userWithCognito.getIdentityProviderUserId()).isEqualTo(
          ANY_IDENTITY_PROVIDER_ID);
      verify(cognitoClient, never()).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
      verify(cognitoClient).adminCreateUser(argThat(request ->
          request.username().equals(email)
              && request.messageAction() == MessageActionType.SUPPRESS
              && request.userAttributes().stream().anyMatch(
              attr -> attr.name().equals("preferred_username") && attr.value()
                  .equals(ANY_IDENTITY_PROVIDER_ID.toString()))
              && request.userAttributes().stream()
              .anyMatch(attr -> attr.name().equals("name") && attr.value().equals(username))
              && request.userAttributes().stream()
              .anyMatch(attr -> attr.name().equals("email") && attr.value().equals(email))
      ));
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      User user = User.builder().identityProviderUserId(ANY_IDENTITY_PROVIDER_ID).name("TestName")
          .email("test@email.com").build();
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminCreateUser(any(AdminCreateUserRequest.class));

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.createStandardUser(user));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenUserIsAdministrator() {
      // given
      User user = User.builder().identityProviderUserId(ANY_IDENTITY_PROVIDER_ID).owner().build();

      // when
      Throwable throwable = catchThrowable(() -> identityProvider.createStandardUser(user));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
      assertThat(throwable).hasMessage("User can't be Administrator");
    }
  }

  @Nested
  class CreateAdminUser {

    @Nested
    class Preconditions {

      @Nested
      class WhenEmailIsNull {

        @Test
        public void shouldThrowIllegalArgumentException() {
          // given
          String email = null;

          // when
          Throwable throwable = catchThrowable(
              () -> identityProvider
                  .createAdminUser(ANY_IDENTITY_PROVIDER_ID, email, ANY_PASSWORD));

          // then
          assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("'email' cannot be null or empty");
        }
      }

      @Nested
      class WhenEmailIsEmpty {

        @Test
        public void shouldThrowIllegalArgumentException() {
          // given
          String email = "";

          // when
          Throwable throwable = catchThrowable(
              () -> identityProvider
                  .createAdminUser(ANY_IDENTITY_PROVIDER_ID, email, ANY_PASSWORD));

          // then
          assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("'email' cannot be null or empty");
        }
      }

      @Nested
      class WhenPasswordIsNull {

        @Test
        public void shouldThrowIllegalArgumentException() {
          // given
          String password = null;

          // when
          Throwable throwable = catchThrowable(
              () -> identityProvider
                  .createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, password));

          // then
          assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("'password' cannot be null or empty");
        }
      }

      @Nested
      class WhenPasswordIsEmpty {

        @Test
        public void shouldThrowIllegalArgumentException() {
          // given
          String password = "";

          // when
          Throwable throwable = catchThrowable(
              () -> identityProvider
                  .createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, password));

          // then
          assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("'password' cannot be null or empty");
        }
      }
    }

    @Test
    void shouldReturnIdentityProviderIdAndSetPasswordWhenAdminUserDataProvided() {
      // given
      mockPreviousPasswordsAttributeToHaveValue("hash1", ANY_EMAIL);

      // when
      identityProvider.createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, ANY_PASSWORD);

      //then
      verify(cognitoClient).adminSetUserPassword(argThat(req ->
          req.username().equals(ANY_EMAIL)
              && req.password().equals(ANY_PASSWORD)
              && req.permanent()
      ));

      verify(cognitoClient).adminCreateUser(argThat(req ->
          req.messageAction().equals(MessageActionType.SUPPRESS)
              && req.temporaryPassword().equals(ANY_PASSWORD)
              && req.username().equals(ANY_EMAIL)
              && req.userAttributes().size() == 2
              && req.userAttributes().stream().anyMatch(
              attributeType -> attributeType.name().equals("email") && attributeType.value()
                  .equals(ANY_EMAIL))
              && req.userAttributes().stream().anyMatch(
              attributeType -> attributeType.name().equals("preferred_username") && attributeType
                  .value().equals(ANY_IDENTITY_PROVIDER_ID.toString()))
      ));
      verifyThatPreviousPasswordsHaveBeenUpdatedWithLastUpdateTimestamp(2);
    }

    @Test
    public void shouldThrowPasswordInvalidExceptionWhenInvalidPasswordExceptionOccurs() {
      InvalidPasswordException exception = getStubbedInvalidPasswordException();
      doThrow(exception).when(cognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));

      Throwable throwable = catchThrowable(
          () -> identityProvider
              .createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, ANY_PASSWORD));

      assertThat(throwable).isInstanceOf(PasswordInvalidException.class);
      assertThat(throwable).hasMessage("Parameters cannot be processed");
    }

    @Test
    public void shouldThrowPasswordInvalidExceptionWhenInvalidParameterExceptionOccurs() {
      InvalidParameterException exception = getStubbedInvalidParameterException();
      doThrow(exception).when(cognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));

      Throwable throwable = catchThrowable(
          () -> identityProvider
              .createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, ANY_PASSWORD));

      assertThat(throwable).isInstanceOf(PasswordInvalidException.class);
      assertThat(throwable).hasMessage("Parameters cannot be processed");
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminCreateUser(any(AdminCreateUserRequest.class));

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider
              .createAdminUser(ANY_IDENTITY_PROVIDER_ID, ANY_EMAIL, ANY_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }
  }

  @Nested
  class DeleteUser {

    @Nested
    class WhenEmailFoundInCognito {

      @Test
      void shouldReturnUserWithIdAndSetPassword() {
        // given
        String email = "test@email.com";
        mockEmailExistsInCognito();

        // when
        identityProvider.deleteUser(email);

        //then
        verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
      }

      private void mockEmailExistsInCognito() {
        ListUsersResponse nonEmptyListUsersResponse = ListUsersResponse.builder()
            .users(Arrays.asList(UserType.builder().build()))
            .build();
        when(cognitoClient.listUsers(any(ListUsersRequest.class)))
            .thenReturn(nonEmptyListUsersResponse);
      }
    }

    @Nested
    class WhenEmailNotFoundInCognito {

      @Test
      void shouldReturnUserWithIdAndSetPassword() {
        // given
        String email = "test@email.com";
        mockEmailDoesNotExistsInCognito();

        // when
        identityProvider.deleteUser(email);

        //then
        verify(cognitoClient, never()).adminDeleteUser(any(AdminDeleteUserRequest.class));
      }

      private void mockEmailDoesNotExistsInCognito() {
        ListUsersResponse nonEmptyListUsersResponse = ListUsersResponse.builder()
            .users(new ArrayList())
            .build();
        when(cognitoClient.listUsers(any(ListUsersRequest.class)))
            .thenReturn(nonEmptyListUsersResponse);
      }
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      String email = "test@email.com";
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .listUsers(any(ListUsersRequest.class));

      // when
      Throwable throwable = catchThrowable(() -> identityProvider.deleteUser(email));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }
  }

  @Nested
  class VerifyEmail {

    @Captor
    private ArgumentCaptor<AdminUpdateUserAttributesRequest> updateUserAttributesRequestCaptor;

    @Test
    void shouldCallIdentityProviderWithProvidedEmailForUser() {
      // given
      String email = "test@email.com";
      User user = User.builder().name("TestName").email(email).build();

      // when
      identityProvider.verifyEmail(user);

      //then
      verify(cognitoClient).adminUpdateUserAttributes(updateUserAttributesRequestCaptor.capture());
      assertThat(updateUserAttributesRequestCaptor.getValue())
          .isEqualTo(expectedVerifyEmailRequest(email));
    }

    @Test
    void shouldCallIdentityProviderWithProvidedEmailForUserEntity() {
      // given
      String email = "test@email.com";
      UserEntity user = UserEntity.builder().name("TestName").email(email).build();

      // when
      identityProvider.verifyEmail(user);

      //then
      verify(cognitoClient).adminUpdateUserAttributes(updateUserAttributesRequestCaptor.capture());
      assertThat(updateUserAttributesRequestCaptor.getValue())
          .isEqualTo(expectedVerifyEmailRequest(email));
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      User user = createAdminUser();
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));

      // when
      Throwable throwable = catchThrowable(() -> identityProvider.verifyEmail(user));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }


    private AdminUpdateUserAttributesRequest expectedVerifyEmailRequest(String email) {
      return AdminUpdateUserAttributesRequest.builder()
          .userPoolId(USER_POOL_ID)
          .username(email)
          .userAttributes(AttributeType.builder()
              .name("email_verified")
              .value("true")
              .build())
          .build();
    }
  }

  @Nested
  class LoginUser {

    private String ANY_EMAIL = "test@email.com";
    private ArgumentCaptor<AdminInitiateAuthRequest> authRequestArgumentCaptor =
        ArgumentCaptor.forClass(AdminInitiateAuthRequest.class);

    @Test
    void shouldLoginUser() {
      // given
      String tmpUserName = "tmpUserName@example.com";
      String tmpPass = "tmpPass";
      mockCognitoGetUserCallForEmail(tmpUserName, "true");
      mockSuccessfulInitAuthCall();

      // when
      UUID result = identityProvider.loginUser(tmpUserName, tmpPass);

      // then
      assertThat(result).isEqualTo(ANY_IDENTITY_PROVIDER_ID);
      verify(cognitoClient).adminInitiateAuth(authRequestArgumentCaptor.capture());
      AdminInitiateAuthRequest request = authRequestArgumentCaptor.getValue();
      assertThat(request.authParameters()).containsOnlyKeys("USERNAME", "PASSWORD", "SECRET_HASH");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenChallengeIsReturnedUponAuthCall() {
      // given
      mockChallengeInAuthResponse();

      // when
      Throwable result = catchThrowable(() ->
          identityProvider.loginUser("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(result).isInstanceOf(IllegalStateException.class);
      assertThat(result).hasMessageStartingWith("No challenges expected");
    }

    @Test
    void shouldThrowEmailNotConfirmedExceptionWhenEmailNotVerified() {
      // given
      String email = "tmpUserName@example.com";
      String password = "tmpPass";
      mockCognitoGetUserCallForEmail(ANY_EMAIL, "false");
      mockSuccessfulInitAuthCall();

      // when
      Throwable result = catchThrowable(() -> identityProvider.loginUser(email, password));

      // then
      assertThat(result).isInstanceOf(EmailNotConfirmedException.class);
      assertThat(result).hasMessage("Email not confirmed");
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenUserIsNotFound() {
      // given
      when(cognitoClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
          .thenThrow(getStubbedUserNotFoundException());

      // when
      Throwable result = catchThrowable(() ->
          identityProvider.loginUser("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(result).isInstanceOf(InvalidCredentialsException.class);
      assertThat(result).hasMessage("Invalid credentials");
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminInitiateAuth(any(AdminInitiateAuthRequest.class));

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.loginUser(ANY_EMAIL, ANY_PASSWORD));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    public void shouldThrowNotAuthorizedExceptionWhenUserProvidedIncorrectCredentials() {
      // given
      when(cognitoClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
          .thenThrow(getStubbedNotAuthorizedException());

      // when
      Throwable result = catchThrowable(() ->
          identityProvider.loginUser("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(result).isInstanceOf(NotAuthorizedException.class);
      assertThat(result.getMessage()).contains("Incorrect username or password.");
    }

    private void mockChallengeInAuthResponse() {
      when(cognitoClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
          .thenReturn(AdminInitiateAuthResponse.builder()
              .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
              .build()
          );
    }

    private void mockSuccessfulInitAuthCall() {
      when(cognitoClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
          .thenReturn(AdminInitiateAuthResponse.builder().build());
    }

    private void mockCognitoGetUserCallForEmail(String tmpUserName, String emailVerified) {
      when(cognitoClient.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
          AdminGetUserResponse.builder()
              .username(tmpUserName)
              .userAttributes(Arrays.asList(
                  AttributeType.builder().name("email").value(tmpUserName).build(),
                  AttributeType.builder().name("sub").value(ANY_SUB_ID.toString()).build(),
                  AttributeType.builder().name("preferred_username")
                      .value(ANY_IDENTITY_PROVIDER_ID.toString()).build(),
                  AttributeType.builder().name("email_verified").value(emailVerified).build()
              ))
              .build()
      );
    }
  }

  @Nested
  class SetUserName {

    @Test
    void shouldCallCognitoClient() {
      // given
      String email = "test@example.com";
      String name = "new name";

      // when
      identityProvider.setUserName(email, name);

      // then
      verify(cognitoClient, times(1)).adminUpdateUserAttributes(any());
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));
      // when
      Throwable throwable = catchThrowable(() ->
          identityProvider.setUserName("tmpUserName@example.com", "newName"));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }
  }

  @Nested
  class SetUserPassword {

    @Test
    void shouldLoginUserAndUpdateLastPasswordsWithLastUpdateTimestamp() {
      // given
      String email = "test@example.com";
      String password = "Password";
      mockCognitoSetUserPassword(email, password);
      mockPreviousPasswordsAttributeToHaveValue("hash1,hash2", email);

      // when
      identityProvider.setUserPassword(email, password);

      // then
      verify(cognitoClient, times(1)).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
      verifyThatPreviousPasswordsHaveBeenUpdatedWithLastUpdateTimestamp(3);
    }

    @Test
    void shouldLoginUserAndUpdateLastPasswordsWhenThereAre5PreviousPasswords() {
      // given
      String email = "test@example.com";
      String password = "Password";
      mockCognitoSetUserPassword(email, password);
      mockPreviousPasswordsAttributeToHaveValue("hash1,hash2,hash3,hash4,hash5", email);

      // when
      identityProvider.setUserPassword(email, password);

      // then
      verify(cognitoClient, times(1)).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
      verifyThatPreviousPasswordsHaveBeenUpdatedWithLastUpdateTimestamp(5);
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenUserIsUnauthorised() {
      // given
      when(cognitoClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
          .thenThrow(getStubbedNotAuthorizedException());

      // when
      Throwable result = catchThrowable(() ->
          identityProvider.setUserPassword("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(result).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(result).hasMessage("External Service Failure");
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenUserIsNotFound() {
      // given
      when(cognitoClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
          .thenThrow(getStubbedUserNotFoundException());

      // when
      Throwable result = catchThrowable(() ->
          identityProvider.setUserPassword("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(result).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(result).hasMessage("External Service Failure");
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
      // given
      doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
          .adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
      // when
      Throwable throwable = catchThrowable(() ->
          identityProvider.setUserPassword("tmpUserName@example.com", "tmpPass"));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    public void shouldThrowPasswordInvalidExceptionWhenInvalidPasswordExceptionOccurs() {
      InvalidPasswordException exception = getStubbedInvalidPasswordException();
      doThrow(exception).when(cognitoClient)
          .adminSetUserPassword(any(AdminSetUserPasswordRequest.class));

      Throwable throwable = catchThrowable(() ->
          identityProvider.setUserPassword("tmpUserName@example.com", "tmpPass"));

      assertThat(throwable).isInstanceOf(PasswordInvalidException.class);
      assertThat(throwable).hasMessage("Parameters cannot be processed");
    }

    @Test
    public void shouldThrowPasswordInvalidExceptionWhenInvalidParameterExceptionOccurs() {
      InvalidParameterException exception = getStubbedInvalidParameterException();
      doThrow(exception).when(cognitoClient)
          .adminSetUserPassword(any(AdminSetUserPasswordRequest.class));

      Throwable throwable = catchThrowable(() ->
          identityProvider.setUserPassword("tmpUserName@example.com", "tmpPass"));

      assertThat(throwable).isInstanceOf(PasswordInvalidException.class);
      assertThat(throwable).hasMessage("Parameters cannot be processed");
    }
  }

  @Nested
  class GetUserAsUserEntity {

    @Test
    public void shouldReturnUserEntityWhenUserExistsAndUseCreationDateAsPasswordLastUpdate() {
      // given
      when(cognitoClient.adminGetUser(any())).thenReturn(
          createAdminGetUserResponseWithoutPasswordLastUpdate());

      // when
      UserEntity user = identityProvider.getUserAsUserEntity(ANY_EMAIL);

      // then
      verifyResponseAndPasswordLastUpdate(user, LocalDateTime.of(2020, 1, 10, 10, 15, 30));
    }

    @Test
    public void shouldReturnUserEntityWhenUserExistsAndTakePasswordLastUpdateTimestamp() {
      // given
      when(cognitoClient.adminGetUser(any()))
          .thenReturn(createAdminGetUserResponseWithPasswordLastUpdate());

      // when
      UserEntity user = identityProvider.getUserAsUserEntity(ANY_EMAIL);

      // then
      verifyResponseAndPasswordLastUpdate(user, LocalDateTime.of(2020, 5, 20, 11, 55, 33));
    }

    private AdminGetUserResponse createAdminGetUserResponseWithoutPasswordLastUpdate() {
      return createAdminGetUserResponseWithPasswordLastUpdate(null);
    }

    private AdminGetUserResponse createAdminGetUserResponseWithPasswordLastUpdate() {
      return createAdminGetUserResponseWithPasswordLastUpdate("2020-05-20T11:55:33");
    }

    private AdminGetUserResponse createAdminGetUserResponseWithPasswordLastUpdate(
        String passwordLastUpdateTimestamp) {
      val attrs = newArrayList(
          AttributeType.builder().name("email").value(ANY_EMAIL).build(),
          AttributeType.builder().name("preferred_username")
              .value(ANY_IDENTITY_PROVIDER_ID.toString()).build(),
          AttributeType.builder().name("email_verified").value(Boolean.TRUE.toString()).build());
      if (passwordLastUpdateTimestamp != null) {
        attrs.add(
            AttributeType.builder().name("custom:password-lu-tstmp")
                .value(passwordLastUpdateTimestamp)
                .build());
      }
      return AdminGetUserResponse.builder()
          .userAttributes(attrs)
          .userCreateDate(Instant.parse("2020-01-10T10:15:30.00Z"))
          .build();
    }

    private void verifyResponseAndPasswordLastUpdate(UserEntity user,
        LocalDateTime expectedTimestamp) {
      assertThat(user).isNotNull();
      assertThat(user.getId()).isNull();
      assertThat(user.getIdentityProviderUserId()).isEqualTo(ANY_IDENTITY_PROVIDER_ID);
      assertThat(user.isEmailVerified()).isTrue();
      assertThat(user.getEmail()).isEqualTo(ANY_EMAIL);
      assertThat(user.getPasswordUpdateTimestamp()).isEqualToIgnoringNanos(expectedTimestamp);
    }
  }

  @Nested
  class GetUserDetailsByIdentityProviderId {

    private String ANY_EMAIL = "test@email.com";
    private String ANY_NAME = "Any Name";
    private List<UserType> NON_EMPTY_COLLECTION = Arrays.asList(
        UserType.builder().attributes(
            AttributeType.builder().name("email").value(ANY_EMAIL).build(),
            AttributeType.builder().name("name").value(ANY_NAME).build()
        ).build());
    private List<UserType> EMPTY_COLLECTION = new ArrayList();

    @Test
    void shouldReturnUserWhenIdentityProviderUserFound() {
      User user = createStandardUser();
      User expectedUserResult = createUserWithDetails(user);
      ListUsersResponse nonEmptyListUsersResponse = ListUsersResponse.builder()
          .users(NON_EMPTY_COLLECTION).build();

      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(nonEmptyListUsersResponse);

      assertThat(identityProvider.getUserDetailsByIdentityProviderId(user))
          .isEqualTo(expectedUserResult);
    }

    @Test
    void shouldThrowExceptionWhenIdentityProviderUserNotFound() {
      // given
      User user = createStandardUser();
      ListUsersResponse emptyListUsersResponse = ListUsersResponse.builder()
          .users(EMPTY_COLLECTION).build();
      when(cognitoClient.listUsers(any(ListUsersRequest.class)))
          .thenReturn(emptyListUsersResponse);

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.getUserDetailsByIdentityProviderId(user));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      assertThat(throwable).hasMessage("External Service Failure");
    }

    @Test
    void shouldThrowExceptionWhenIdentityProviderUserIdIsNull() {
      // given
      User user = User.builder().id(UUID.randomUUID()).build();

      // when
      Throwable throwable = catchThrowable(
          () -> identityProvider.getUserDetailsByIdentityProviderId(user));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class);
      assertThat(throwable).hasMessage("'identityProviderUserId' cannot be null");
    }

    private User createUserWithDetails(User user) {
      return user.toBuilder()
          .email(ANY_EMAIL)
          .name(ANY_NAME)
          .build();
    }
  }

  @Nested
  class LockoutMechanism {

    static final String CUSTOM_FAILED_LOGINS = "custom:failed-logins";
    static final String CUSTOM_LOCKOUT_TIME = "custom:lockout-time";

    @Nested
    class IncreaseFailedLoginsByOne {

      @Captor
      private ArgumentCaptor<AdminUpdateUserAttributesRequest> updateUserAttributesRequestCaptor;

      @Test
      void shouldIncreaseFailedLoginsByOne() {
        // given
        mockAdminGetUser();

        // when
        identityProvider.increaseFailedLoginsByOne(ANY_EMAIL);

        //then
        verify(cognitoClient)
            .adminUpdateUserAttributes(updateUserAttributesRequestCaptor.capture());
        assertThat(updateUserAttributesRequestCaptor.getValue())
            .isEqualTo(expectedOneFailedLogins());
      }

      @Test
      public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
        //
        mockAdminGetUser();
        doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
            .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));

        // when
        Throwable throwable = catchThrowable(
            () -> identityProvider.increaseFailedLoginsByOne(ANY_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
        assertThat(throwable).hasMessage("External Service Failure");
      }
    }

    @Nested
    class SetLockoutTime {

      @Captor
      private ArgumentCaptor<AdminUpdateUserAttributesRequest> updateUserAttributesRequestCaptor;

      @Test
      void shouldSetLockoutTime() {
        // given
        mockAdminGetUser();

        // when
        identityProvider.setLockoutTime(ANY_EMAIL);

        //then
        verify(cognitoClient)
            .adminUpdateUserAttributes(updateUserAttributesRequestCaptor.capture());
        assertThat(updateUserAttributesRequestCaptor.getValue())
            .isEqualTo(
                expectedLockoutTime(updateUserAttributesRequestCaptor.getValue()
                    .userAttributes().get(0).value()));
      }

      @Test
      public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
        //
        mockAdminGetUser();
        doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
            .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));

        // when
        Throwable throwable = catchThrowable(
            () -> identityProvider.setLockoutTime(ANY_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
        assertThat(throwable).hasMessage("External Service Failure");
      }
    }

    @Nested
    class ResetFailedLoginsAndLockoutTime {

      @Captor
      private ArgumentCaptor<AdminUpdateUserAttributesRequest> updateUserAttributesRequestCaptor;

      @Test
      void shouldSetFailedLoginsToZeroAndResetLockoutTime() {
        // given
        mockAdminGetUser();

        // when
        identityProvider.resetFailedLoginsAndLockoutTime(ANY_EMAIL);

        //then
        verify(cognitoClient)
            .adminUpdateUserAttributes(updateUserAttributesRequestCaptor.capture());
        assertThat(updateUserAttributesRequestCaptor.getValue())
            .isEqualTo(expectedOneFailedLoginsAndResetLockoutTime());
      }

      @Test
      public void shouldThrowIdentityProviderUnavailableExceptionWhenCognitoThrowsException() {
        //
        mockAdminGetUser();
        doThrow(getStubbedCognitoIdentityProviderException()).when(cognitoClient)
            .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));

        // when
        Throwable throwable = catchThrowable(
            () -> identityProvider.resetFailedLoginsAndLockoutTime(ANY_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
        assertThat(throwable).hasMessage("External Service Failure");
      }
    }

    @Nested
    class GetCurrentFailedLogins {

      @Test
      public void shouldReturnCurrentFailedLogins() {
        // given
        mockAdminGetUser();

        // when
        int currentFailedLogins = identityProvider.getCurrentFailedLogins(ANY_EMAIL);

        // then
        assertThat(currentFailedLogins).isNotNull();
        assertThat(currentFailedLogins).isEqualTo(0);
      }
    }

    @Nested
    class GetCurrentLockoutTimeout {

      @Test
      public void shouldReturnCurrentLockoutTime() {
        // given
        mockAdminGetUser();

        // when
        Optional<LocalDateTime> currentLockoutTimeout = identityProvider
            .getCurrentLockoutTime(ANY_EMAIL);

        // then
        assertThat(currentLockoutTimeout).isNotNull();
        assertThat(currentLockoutTimeout.get())
            .isBetween(parse(LOCKOUT_TIME).minusMinutes(5), parse(LOCKOUT_TIME).plusMinutes(5));
      }
    }

    private void mockAdminGetUser() {
      when(cognitoClient.adminGetUser(any())).thenReturn(createAdminGetUserResponse());
    }

    private AdminUpdateUserAttributesRequest expectedOneFailedLogins() {
      return AdminUpdateUserAttributesRequest.builder()
          .userPoolId(USER_POOL_ID)
          .username(ANY_EMAIL)
          .userAttributes(AttributeType.builder()
              .name(CUSTOM_FAILED_LOGINS)
              .value("1")
              .build())
          .build();
    }

    private AdminUpdateUserAttributesRequest expectedLockoutTime(String lockoutTime) {
      return AdminUpdateUserAttributesRequest.builder()
          .userPoolId(USER_POOL_ID)
          .username(ANY_EMAIL)
          .userAttributes(
              AttributeType.builder()
                  .name(CUSTOM_LOCKOUT_TIME)
                  .value(lockoutTime)
                  .build()
          )
          .build();
    }

    private AdminUpdateUserAttributesRequest expectedOneFailedLoginsAndResetLockoutTime() {
      return AdminUpdateUserAttributesRequest.builder()
          .userPoolId(USER_POOL_ID)
          .username(ANY_EMAIL)
          .userAttributes(
              AttributeType.builder()
                  .name(CUSTOM_FAILED_LOGINS)
                  .value("0")
                  .build(),
              AttributeType.builder()
                  .name(CUSTOM_LOCKOUT_TIME)
                  .value("")
                  .build()
          )
          .build();
    }

    private AdminGetUserResponse createAdminGetUserResponse() {
      return AdminGetUserResponse.builder()
          .userAttributes(
              AttributeType.builder().name("email").value(ANY_EMAIL).build(),
              AttributeType.builder().name(CUSTOM_FAILED_LOGINS).value("0").build(),
              AttributeType.builder().name(CUSTOM_LOCKOUT_TIME)
                  .value(LOCKOUT_TIME).build(),
              AttributeType.builder().name("preferred_username")
                  .value(ANY_IDENTITY_PROVIDER_ID.toString()).build(),
              AttributeType.builder().name("email_verified").value(Boolean.TRUE.toString())
                  .build()
          )
          .userCreateDate(Instant.parse("2020-01-10T10:15:30.00Z"))
          .build();
    }
  }

  @Nested
  class LastPasswords {

    @Test
    public void shouldReturnLastPasswords() {
      String email = RandomStringUtils.randomAlphabetic(10);
      mockLastPasswords(Lists.newArrayList("pass1@", "pass2!"));

      List<String> passwords = identityProvider.previousPasswordsForUser(email);

      assertThat(passwords).hasSize(2);
      assertThat(passwords).containsExactlyInAnyOrder("pass1@", "pass2!");
    }

    @Test
    public void clearLastPasswordsShouldTriggerCognitoClient() {
      String email = RandomStringUtils.randomAlphabetic(10);
      mockLastPasswords(Lists.newArrayList("pass1@", "pass2!"));

      identityProvider.clearPreviousPasswordsForUser(email);

      verify(cognitoClient).adminUpdateUserAttributes(any());
    }

    private void mockLastPasswords(List<String> passwords) {
      AdminGetUserResponse resp = getUserResponse(passwords);
      when(cognitoClient.adminGetUser(any())).thenReturn(resp);
    }

    private AdminGetUserResponse getUserResponse(List<String> passwords) {
      return AdminGetUserResponse.builder()
          .userAttributes(
              AttributeType.builder()
                  .name("custom:previous-passwords")
                  .value(String.join(",", passwords))
                  .build())
          .build();
    }
  }

  @Nested
  class CloneUser {

    @Test
    public void shouldCreateNewUserWithPassedEmailAndExternalId() {
      // given
      ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor
          .forClass(AdminCreateUserRequest.class);
      String currentEmail = "a@b.com";
      String newEmail = "c@b.com";
      UUID currentIdentityProviderId = UUID.fromString("aacfffeb-cae8-4029-ae14-6fa15acbc1f1");
      UUID newIdentityProviderId = UUID.fromString("310b331f-8c3d-4887-a367-adbd72424900");
      mockExistingUser(currentEmail, currentIdentityProviderId);

      // when
      identityProvider.cloneUserAndSetEmailTo(currentIdentityProviderId, newIdentityProviderId,
          newEmail);

      // then
      verify(cognitoClient).adminCreateUser(captor.capture());
      AdminCreateUserRequest request = captor.getValue();

      assertThat(request.username()).isEqualTo(newEmail);
      assertThat(request.messageAction()).isEqualTo(MessageActionType.SUPPRESS);
      assertThat(request.userAttributes()).contains(getEmailAttribute(newEmail));
      assertThat(request.userAttributes()).contains(getEmailAttribute(newEmail));
    }

    @Test
    public void shouldCreateNewUserWithPassedName() {
      // given
      ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor
          .forClass(AdminCreateUserRequest.class);
      String currentEmail = "a@b.com";
      String newEmail = "c@b.com";
      String name = "myname";
      UUID currentIdentityProviderId = UUID.fromString("aacfffeb-cae8-4029-ae14-6fa15acbc1f1");
      UUID newIdentityProviderId = UUID.fromString("310b331f-8c3d-4887-a367-adbd72424900");
      mockExistingUserWithName(currentEmail, currentIdentityProviderId, name);

      // when
      identityProvider.cloneUserAndSetEmailTo(currentIdentityProviderId, newIdentityProviderId,
          newEmail);

      // then
      verify(cognitoClient).adminCreateUser(captor.capture());
      AdminCreateUserRequest request = captor.getValue();

      assertThat(request.username()).isEqualTo(newEmail);
      assertThat(request.messageAction()).isEqualTo(MessageActionType.SUPPRESS);
      assertThat(request.userAttributes()).contains(getEmailAttribute(newEmail));
      assertThat(request.userAttributes())
          .contains(AttributeType.builder().name("name").value(name).build());
      assertThat(request.userAttributes()).contains(
          AttributeType.builder().name("preferred_username").value(newIdentityProviderId.toString())
              .build());
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionUponFailedCall() {
      // given
      ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor
          .forClass(AdminCreateUserRequest.class);
      String currentEmail = "a@b.com";
      UUID currentIdentityProviderId = UUID.fromString("aacfffeb-cae8-4029-ae14-6fa15acbc1f1");
      UUID newIdentityProviderId = UUID.fromString("310b331f-8c3d-4887-a367-adbd72424900");
      mockExistingUser(currentEmail, currentIdentityProviderId);
      mockExceptionUponCognitoClientCall();

      // when
      Throwable throwable = catchThrowable(() -> identityProvider
          .cloneUserAndSetEmailTo(currentIdentityProviderId, newIdentityProviderId,
              "ala@kota.com"));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
    }

    private void mockExceptionUponCognitoClientCall() {
      when(cognitoClient.adminCreateUser(any()))
          .thenThrow(CognitoIdentityProviderException.builder().awsErrorDetails(
              AwsErrorDetails.builder().errorCode("errorcode").errorMessage("massage").build())
              .build());
    }

    private void mockExistingUserWithName(String currentEmail, UUID currentIdentityProviderId,
        String name) {
      mockListUsersResponse(currentEmail);
      mockAdminGetUserResponse(
          AttributeType.builder().name("name").value(name).build(),
          AttributeType.builder().name("email").value(currentEmail).build(),
          AttributeType.builder().name("preferred_username")
              .value(currentIdentityProviderId.toString()).build()
      );
    }

    private void mockExistingUser(String currentEmail, UUID currentIdentityProviderId) {
      mockListUsersResponse(currentEmail);
      mockAdminGetUserResponse(
          AttributeType.builder().name("email").value(currentEmail).build(),
          AttributeType.builder().name("preferred_username")
              .value(currentIdentityProviderId.toString()).build()
      );
    }

    private void mockAdminGetUserResponse(AttributeType... attributes) {
      when(cognitoClient.adminGetUser(any())).thenReturn(
          AdminGetUserResponse.builder()
              .userAttributes(attributes)
              .userCreateDate(Instant.now())
              .build()
      );
    }

    private void mockListUsersResponse(String currentEmail) {
      when(cognitoClient.listUsers(any())).thenReturn(
          ListUsersResponse.builder()
              .users(UserType.builder().attributes(
                  getEmailAttribute(currentEmail)).build())
              .build()
      );
    }

    private AttributeType getEmailAttribute(String currentEmail) {
      return AttributeType.builder().name("email").value(currentEmail).build();
    }
  }

  @Nested
  class IsUserBetaTester {

    private List<GroupType> NON_EMPTY_COLLECTION = Arrays
        .asList(GroupType.builder().groupName("NonBetaTeser").build());
    private List<GroupType> EMPTY_COLLECTION = new ArrayList();

    private AdminListGroupsForUserResponse nonBetaTesterGroupsListForUserResponse =
        AdminListGroupsForUserResponse.builder()
            .groups(NON_EMPTY_COLLECTION).build();
    private AdminListGroupsForUserResponse emptyGroupsListForUserResponse =
        AdminListGroupsForUserResponse.builder()
            .groups(EMPTY_COLLECTION).build();

    @ParameterizedTest
    @ValueSource(strings = {"accounts.beta-testers.dev", "accounts.beta-testers.st",
        "accounts.beta-testers.sit", "accounts.beta-testers.prod"})
    public void shouldReturnTrueWhenUUserBelongsToBetaTestersGroup(String betaTestersGroup) {
      AdminListGroupsForUserResponse adminListGroupsForUsersResponse =
          AdminListGroupsForUserResponse.builder()
              .groups(Arrays.asList(GroupType.builder().groupName(betaTestersGroup).build()))
              .build();
      when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
          .thenReturn(adminListGroupsForUsersResponse);

      assertThat(identityProvider.isUserBetaTester(ANY_EMAIL)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotBelongsToBetaTestersGroup() {
      when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
          .thenReturn(nonBetaTesterGroupsListForUserResponse);

      assertThat(identityProvider.isUserBetaTester(ANY_EMAIL)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotBelongsToAnyGroup() {
      when(cognitoClient.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
          .thenReturn(emptyGroupsListForUserResponse);

      assertThat(identityProvider.isUserBetaTester(ANY_EMAIL)).isFalse();
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmailIsNull() {
      String email = null;

      // when
      Throwable throwable = catchThrowable(() -> identityProvider.isUserBetaTester(email));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
      assertThat(throwable).hasMessage("'email' cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEmailIsEmpty() {
      String email = "";

      // when
      Throwable throwable = catchThrowable(() -> identityProvider.isUserBetaTester(email));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
      assertThat(throwable).hasMessage("'email' cannot be null or empty");
    }
  }

  private void mockCognitoSetUserPassword(String email, String password) {
    AdminSetUserPasswordRequest adminSetUserPasswordRequest = AdminSetUserPasswordRequest
        .builder()
        .userPoolId(USER_POOL_ID)
        .password(password)
        .username(email)
        .permanent(true)
        .build();

    when(cognitoClient.adminSetUserPassword(adminSetUserPasswordRequest)).thenReturn(
        AdminSetUserPasswordResponse.builder().build()
    );
  }

  private AdminCreateUserResponse cognitoUserWithoutSubAttribute() {
    return AdminCreateUserResponse.builder()
        .user(UserType.builder().build())
        .build();
  }

  private AdminCreateUserResponse successfullyCreatedCognitoUser() {
    return AdminCreateUserResponse.builder()
        .user(UserType.builder()
            .attributes(
                AttributeType.builder().name("sub").value(ANY_SUB_ID.toString()).build(),
                AttributeType.builder().name("preferred_username")
                    .value(ANY_IDENTITY_PROVIDER_ID.toString()).build()
            ).build())
        .build();
  }

  private User createAdminUser() {
    return User.builder().owner()
        .email("test@email.com")
        .build();
  }

  private User createStandardUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .build();
  }

  private CognitoIdentityProviderException getStubbedCognitoIdentityProviderException() {
    AwsErrorDetails details = getStubbedAwsUserNotFoundErrorDetails();
    CognitoIdentityProviderException exception = (CognitoIdentityProviderException) CognitoIdentityProviderException
        .builder().awsErrorDetails(details).build();

    return exception;
  }

  private InvalidPasswordException getStubbedInvalidPasswordException() {
    AwsErrorDetails details = getStubbedAwsUserNotFoundErrorDetails();
    InvalidPasswordException exception = InvalidPasswordException
        .builder().awsErrorDetails(details).build();

    return exception;
  }

  private InvalidParameterException getStubbedInvalidParameterException() {
    AwsErrorDetails details = getStubbedAwsUserNotFoundErrorDetails();
    InvalidParameterException exception = InvalidParameterException
        .builder().awsErrorDetails(details).build();

    return exception;
  }

  private AwsErrorDetails getStubbedAwsUserNotFoundErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("UserNotFoundException")
        .errorMessage("User does not exist.")
        .build();
  }

  private AwsErrorDetails getStubbedAwsNotAuthorizedErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("NotAuthorizedException")
        .errorMessage("Incorrect username or password.")
        .build();
  }

  private NotAuthorizedException getStubbedNotAuthorizedException() {
    AwsErrorDetails details = getStubbedAwsNotAuthorizedErrorDetails();

    return NotAuthorizedException.builder().awsErrorDetails(details).build();
  }

  private UserNotFoundException getStubbedUserNotFoundException() {
    AwsErrorDetails details = getStubbedAwsUserNotFoundErrorDetails();

    return UserNotFoundException.builder().awsErrorDetails(details).build();
  }

  private void mockPreviousPasswordsAttributeToHaveValue(String previousPasswords, String email) {
    when(cognitoClient.adminGetUser(AdminGetUserRequest.builder()
        .userPoolId(USER_POOL_ID)
        .username(email).build())).thenReturn(AdminGetUserResponse.builder()
        .userAttributes(
            AttributeType.builder().name("custom:previous-passwords").value(previousPasswords)
                .build()).build());
  }

  private void verifyThatPreviousPasswordsHaveBeenUpdatedWithLastUpdateTimestamp(
      int expectedPreviousPasswords) {
    verify(cognitoClient)
        .adminUpdateUserAttributes(updateUserAttributesRequestArgumentCaptor.capture());
    AdminUpdateUserAttributesRequest updateRequest = updateUserAttributesRequestArgumentCaptor
        .getValue();
    AttributeType previousPasswordsCaptured = updateRequest.userAttributes().get(0);
    assertThat(previousPasswordsCaptured.name()).isEqualTo("custom:previous-passwords");
    assertThat(previousPasswordsCaptured.value().split(",")).hasSize(expectedPreviousPasswords);
    for (int i = 1; i < expectedPreviousPasswords; i++) {
      assertThat(previousPasswordsCaptured.value().split(",")[i]).isEqualTo("hash" + i);
    }

    AttributeType passwordLastUpdateTimestampCaptured = updateRequest.userAttributes().get(1);
    assertThat(passwordLastUpdateTimestampCaptured.name())
        .isEqualTo("custom:password-lu-tstmp");
    assertThat(LocalDateTime.parse(passwordLastUpdateTimestampCaptured.value()))
        .isCloseTo(LocalDateTime.now(),
            new TemporalUnitWithinOffset(1, ChronoUnit.SECONDS));
  }
}