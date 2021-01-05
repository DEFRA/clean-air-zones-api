package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String ANY_EMAIL = "dev@jaqu.gov";
  private static final String ANY_PASSWORD = "dev@jaqu.gov";
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();
  private static final UUID ANY_IDENTITY_PROVIDER_ID = UUID.randomUUID();

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private AccountUserRepository accountUserRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RecentlyUsedPasswordChecker recentlyUsedPasswordChecker;

  @Mock
  private UserPermissionsService userPermissionsService;

  @InjectMocks
  private UserService userService;

  @Nested
  class CreateAdminUserForExistingEmail {

    @Test
    public void shouldCreateNewUserInTheDatabaseAndSetPassword() {
      // given
      UserEntity user = createAdminUser();
      mockUserCreationAndPasswordSetup(user);
      String email = user.getEmail();

      // when
      UserEntity alteredUser = userService
          .createAdminUserForExistingEmail(email, ANY_PASSWORD, ANY_ACCOUNT_ID);

      // then
      verify(userRepository).save(any());
      verify(identityProvider).setUserPassword(any(), any());
    }

    private void mockUserCreationAndPasswordSetup(UserEntity user) {
      when(identityProvider.getUserAsUserEntity(any())).thenReturn(user);
      when(userRepository.save(any())).thenReturn(user);
      when(identityProvider.getEmailByIdentityProviderId(any())).thenReturn(user.getEmail());
      doNothing().when(identityProvider).setUserPassword(any(), any());
    }
  }

  @Nested
  class CreateAdminUser {

    @Test
    public void shouldThrowExceptionIfEmailIsNotUnique() {
      // when
      mockNotUniqueEmailConditions();

      //then
      assertThrows(NotUniqueEmailException.class,
          () -> userService.createAdminUser(ANY_EMAIL, ANY_PASSWORD, ANY_ACCOUNT_ID));
      verify(accountUserRepository, never()).insert(any());
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenIdentityProviderThrowsCognitoIdentityProviderException() {
      // given
      doThrow(IdentityProviderUnavailableException.class).when(identityProvider)
          .checkIfUserExists(ANY_EMAIL);
      UserEntity user = createAdminUser();

      // when
      Throwable throwable = catchThrowable(
          () -> userService.createAdminUser(ANY_EMAIL, ANY_PASSWORD, ANY_ACCOUNT_ID));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      verify(accountUserRepository, never()).insert(any());
    }

    @Test
    public void shouldCreateUserInIdentityProviderIfEmailIsUnique() {
      //given
      UUID identityProviderId = UUID.randomUUID();
      UserEntity userWithIdentityProviderId = UserEntity.builder()
          .isOwner(true)
          .email(ANY_EMAIL)
          .accountId(ANY_ACCOUNT_ID)
          .identityProviderUserId(identityProviderId)
          .build();
      UserEntity userWithId = userWithIdentityProviderId.toBuilder().id(UUID.randomUUID()).build();

      when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(false);
//      doNothing().when(identityProvider.createAdminUser(identityProviderId, ANY_EMAIL, ANY_PASSWORD));
      when(userRepository.save(any())).thenReturn(userWithId);

      //when
      UserEntity result = userService.createAdminUser(ANY_EMAIL, ANY_PASSWORD, ANY_ACCOUNT_ID);

      //then
      assertThat(result).isEqualTo(userWithId);
      verify(identityProvider).createAdminUser(identityProviderId, ANY_EMAIL, ANY_PASSWORD);
      verify(userRepository).save(argThat(user ->
          user.getAccountPermissions().size() == 0
              && Objects.isNull(user.getId())
              && Objects.nonNull(user.getIdentityProviderUserId())
              && user.isOwner()
              && user.getEmail().equals(ANY_EMAIL)
              && user.getAccountId().equals(ANY_ACCOUNT_ID)
      ));
    }

    @Test
    public void shouldThrowExceptionWhenUserIsStoredInCognitoButNotInTheDatabase() {
      // when
      mockDataInconsistencyEmailConditions();

      // then
      assertThrows(IdentityProviderUnavailableException.class,
          () -> userService.createAdminUser(ANY_EMAIL, ANY_PASSWORD, any()));
    }
  }

  @Nested
  class CreateStandardUser {

    @Test
    public void shouldThrowIllegalArgumentExceptionIfUserIsOwner() {
      // given
      User user = createStandardUser().toBuilder().isOwner(true).build();

      // when
      Throwable throwable = catchThrowable(() -> userService.createStandardUser(user));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("User cannot be an owner");
      verify(accountUserRepository, never()).insert(any());
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfUserHasInternalIdentifier() {
      // given
      User user = createStandardUser().toBuilder().isOwner(false).id(UUID.randomUUID()).build();

      // when
      Throwable throwable = catchThrowable(() -> userService.createStandardUser(user));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("User id must be null");
      verify(accountUserRepository, never()).insert(any());
    }

    @Test
    public void shouldThrowExceptionIfEmailIsNotUnique() {
      //given
      User user = createStandardUser();

      mockNotUniqueEmailConditions();

      //then
      assertThrows(NotUniqueEmailException.class,
          () -> userService.createStandardUser(user));
      verify(accountUserRepository, never()).insert(any());
    }

    @Test
    public void shouldCreateUserInIdentityProviderIfEmailIsUnique() {
      //given
      User userWithIdentityProviderId = createStandardUser();
      User userWithId = userWithIdentityProviderId.toBuilder()
          .id(UUID.randomUUID())
          .build();

      when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(false);
      when(identityProvider.createStandardUser(userWithId)).thenReturn(userWithId);
      when(accountUserRepository.insert(userWithIdentityProviderId)).thenReturn(userWithId);
      User result = userService.createStandardUser(userWithIdentityProviderId);

      //then
      assertThat(result).isEqualTo(userWithId);
      verify(identityProvider).createStandardUser(eq(userWithId));
      verify(accountUserRepository).insert(userWithIdentityProviderId);
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenIdentityProviderThrowsException() {
      // given
      User user = createStandardUser();
      doThrow(IdentityProviderUnavailableException.class).when(identityProvider)
          .checkIfUserExists(any());

      // when
      Throwable throwable = catchThrowable(() -> userService.createStandardUser(user));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
    }

  }

  @Nested
  class GetCompleteUserDetailsAsUserEntityForAccountUserId {

    @Test
    public void shouldThrowAccountUserNotFoundExceptionWhenSuchUserDoesNotExist() {
      // given
      UUID sampleAccountUserId = UUID.randomUUID();
      when(userRepository.findById(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(
          () -> userService
              .getCompleteUserDetailsAsUserEntityForAccountUserId(sampleAccountUserId));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("AccountUser was not found.");
    }

    @Test
    public void shouldThrowAccountUserNotFoundWhenSuchUserIsNotFoundInThirdPartyService() {
      // given
      UUID sampleAccountUserId = UUID.randomUUID();
      when(userRepository.findById(sampleAccountUserId))
          .thenReturn(Optional.of(identityProviderUser()));
      when(identityProvider.checkIfUserExists(any())).thenReturn(false);

      // when
      Throwable throwable = catchThrowable(
          () -> userService
              .getCompleteUserDetailsAsUserEntityForAccountUserId(sampleAccountUserId));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("Account was not found in third party service");
    }

    @Test
    public void shouldReturnUserDetails() {
      // given
      UserEntity dbUser = dbUser();
      when(identityProvider.checkIfUserExists(any())).thenReturn(true);
      when(identityProvider.getUserAsUserEntity(any())).thenReturn(identityProviderUser());
      when(userRepository.findByIdentityProviderUserId(ANY_IDENTITY_PROVIDER_ID))
          .thenReturn(Optional.of(dbUser));
      when(userRepository.findById(ANY_ACCOUNT_USER_ID))
          .thenReturn(Optional.of(dbUser));

      // when
      UserEntity user = userService
          .getCompleteUserDetailsAsUserEntityForAccountUserId(ANY_ACCOUNT_USER_ID);

      // then
      assertThat(user).isNotNull();
    }

    private UserEntity identityProviderUser() {
      return UserEntity.builder()
          .email(ANY_EMAIL)
          .identityProviderUserId(ANY_IDENTITY_PROVIDER_ID)
          .emailVerified(false)
          .build();
    }

    private UserEntity dbUser() {
      return UserEntity.builder()
          .id(ANY_ACCOUNT_USER_ID)
          .identityProviderUserId(ANY_IDENTITY_PROVIDER_ID)
          .isOwner(true)
          .build();
    }
  }

  @Nested
  class GetCompleteUserDetailsForAccountUserId {

    @Test
    public void shouldThrowAccountUserNotFoundExceptionWhenSuchUserDoesNotExist() {
      // given
      UUID sampleAccountUserId = UUID.randomUUID();
      when(accountUserRepository.findById(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(
          () -> userService.getCompleteUserDetailsForAccountUserId(sampleAccountUserId));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("AccountUser was not found.");
    }

    @Test
    public void shouldThrowAccountUserNotFoundWhenSuchUserIsNotFoundInThirdPartyService() {
      // given
      UUID sampleAccountUserId = UUID.randomUUID();
      when(accountUserRepository.findById(sampleAccountUserId))
          .thenReturn(Optional.of(identityProviderUser()));
      when(identityProvider.checkIfUserExists(any())).thenReturn(false);

      // when
      Throwable throwable = catchThrowable(
          () -> userService.getCompleteUserDetailsForAccountUserId(sampleAccountUserId));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("Account was not found in third party service");
    }

    @Test
    public void shouldReturnUserDetails() {
      // given
      User dbUser = dbUser();
      when(identityProvider.checkIfUserExists(any())).thenReturn(true);
      when(identityProvider.getUser(any())).thenReturn(identityProviderUser());
      when(accountUserRepository.findByUserId(ANY_IDENTITY_PROVIDER_ID))
          .thenReturn(Optional.of(dbUser));
      when(accountUserRepository.findById(ANY_ACCOUNT_USER_ID))
          .thenReturn(Optional.of(dbUser));

      // when
      User user = userService.getCompleteUserDetailsForAccountUserId(ANY_ACCOUNT_USER_ID);

      // then
      assertThat(user).isNotNull();
    }

    private User identityProviderUser() {
      return User.builder()
          .email(ANY_EMAIL)
          .identityProviderUserId(ANY_IDENTITY_PROVIDER_ID)
          .emailVerified(false)
          .build();
    }

    private User dbUser() {
      return User.builder()
          .id(ANY_ACCOUNT_USER_ID)
          .identityProviderUserId(ANY_IDENTITY_PROVIDER_ID)
          .isOwner(true)
          .build();
    }
  }

  @Nested
  class GetUserByEmail {

    @Test
    public void shouldReturnOptionalEmptyIfUserIsNotPresentInIdentityProvider() {
      when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(false);

      Optional<User> user = userService.getUserByEmail(ANY_EMAIL);

      assertThat(user).isEmpty();
      verify(accountUserRepository, never()).findByUserId(any());
      verifyNoMoreInteractions(identityProvider);
    }

    @Test
    public void shouldReturnOptionalUserWhenUserIsFoundInTheDatabase() {
      User user = createStandardUser();
      when(identityProvider.checkIfUserExists(any())).thenReturn(true);
      when(identityProvider.getUser(any())).thenReturn(user);
      when(accountUserRepository.findByUserId(any())).thenReturn(Optional.of(user));

      Optional<User> fetchedUser = userService.getUserByEmail(ANY_EMAIL);

      verify(identityProvider).getUser(any());
      verify(accountUserRepository).findByUserId(any());
      assertThat(fetchedUser.isPresent()).isTrue();
    }
  }

  @Nested
  class GetAllUsersForAccountId {

    @Test
    public void shouldReturnEmptyListIfNoUserFound() {
      when(accountUserRepository.findAllUsersByAccountId(any()))
          .thenReturn(Collections.emptyList());

      List<User> users = userService.getAllUsersForAccountId(ANY_ACCOUNT_ID);

      assertThat(users).isEmpty();
    }

    @Test
    public void shouldReturnOptionalUserWhenUserIsFoundInTheDatabase() {
      User user = buildUserFromDB();
      List<User> accountUsers = Arrays.asList(user);
      User userFromIdentityProvider = buildUserWithIdentityProviderUser(user);

      when(identityProvider.getUserDetailsByIdentityProviderId(user))
          .thenReturn(userFromIdentityProvider);
      when(accountUserRepository.findAllUsersByAccountId(any())).thenReturn(accountUsers);

      List<User> users = userService.getAllUsersForAccountId(ANY_ACCOUNT_ID);

      assertThat(users).isNotEmpty();
      verify(identityProvider).getUserDetailsByIdentityProviderId(user);
      verify(accountUserRepository).findAllUsersByAccountId(ANY_ACCOUNT_ID);
    }

    @Test
    public void shouldNotFetchDataFromIdentityProviderWhenUserIsRemoved() {
      User user = getRemovedUser();
      List<User> accountUsers = Arrays.asList(user);

      when(accountUserRepository.findAllUsersByAccountId(ANY_ACCOUNT_ID)).thenReturn(accountUsers);

      List<User> result = userService.getAllUsersForAccountId(ANY_ACCOUNT_ID);

      verify(identityProvider, never()).getUserDetailsByIdentityProviderId(any());
      assertThat(result).isNotEmpty();
    }

    private User getRemovedUser() {
      return User.builder()
          .id(UUID.randomUUID())
          .accountId(ANY_ACCOUNT_ID)
          .identityProviderUserId(null)
          .build();
    }
  }

  @Nested
  class GetUserForAccountId {

    @Test
    public void shouldReturnEmptyListIfNoUserFound() {
      when(userRepository.findByIdAndAccountId(any(), any()))
          .thenReturn(Optional.empty());

      Optional<User> user = userService.getUserForAccountId(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID);

      assertThat(user).isEmpty();
    }

    @Test
    public void shouldReturnOptionalUserWhenUserIsFoundInTheDatabase() {
      UserEntity userEntity = buildUserEntity();
      User user = buildUserFromUserEntity(userEntity);
      User userFromIdentityProvider = buildUserWithIdentityProviderUser(user);

      when(userRepository.findByIdAndAccountId(any(), any())).thenReturn(
          Optional.of(userEntity));
      when(identityProvider.getUserDetailsByIdentityProviderId(user))
          .thenReturn(userFromIdentityProvider);

      Optional<User> result = userService.getUserForAccountId(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID);

      assertThat(result).isNotEmpty();
      assertThat(result.get()).isEqualTo(userFromIdentityProvider);
      verify(identityProvider).getUserDetailsByIdentityProviderId(user);
      verify(userRepository).findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID);
    }
  }

  @Nested
  class SetPassword {

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenEmailNotFound() {
      //given
      UserEntity user = createAdminUser();
      String password = "Password";
      doThrow(IdentityProviderUnavailableException.class)
          .when(identityProvider).getEmailByIdentityProviderId(user.getIdentityProviderUserId());

      // when
      Throwable throwable = catchThrowable(
          () -> userService.setPassword(user, password));

      //then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
      verify(identityProvider, never()).setUserPassword(any(), any());
    }

    @Test
    public void shouldThrowIdentityProviderUnavailableExceptionWhenIdentityProviderThrowsException() {
      // given
      String email = "email@test.com";
      String password = "Password";
      UserEntity user = createAdminUser();
      when(identityProvider.getEmailByIdentityProviderId(user.getIdentityProviderUserId()))
          .thenReturn(email);
      doThrow(IdentityProviderUnavailableException.class)
          .when(identityProvider).setUserPassword(email, password);

      // when
      Throwable throwable = catchThrowable(() -> userService.setPassword(user, password));

      // then
      assertThat(throwable).isInstanceOf(IdentityProviderUnavailableException.class);
    }

    @Test
    public void shouldVerifyUserInIdentityProviderAccountUserWasFound() {
      //given
      String email = "email@test.com";
      String password = "Password";
      UserEntity user = createAdminUser();
      when(identityProvider.getEmailByIdentityProviderId(user.getIdentityProviderUserId()))
          .thenReturn(email);
      doNothing().when(identityProvider).setUserPassword(email, password);

      //when
      userService.setPassword(user, password);

      //then
      verify(identityProvider).setUserPassword(email, password);
    }
  }

  private void mockDataInconsistencyEmailConditions() {
    UUID uuid = UUID.randomUUID();
    User user = User.builder().identityProviderUserId(uuid).email(ANY_EMAIL).build();

    when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(true);
    when(identityProvider.getUser(ANY_EMAIL)).thenReturn(user);
    when(accountUserRepository.findByUserId(uuid)).thenReturn(Optional.empty());
  }

  private void mockNotUniqueEmailConditions() {
    UUID uuid = UUID.randomUUID();
    User user = User.builder().identityProviderUserId(uuid).email(ANY_EMAIL).build();

    when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(true);
    when(identityProvider.getUser(ANY_EMAIL)).thenReturn(user);
    when(accountUserRepository.findByUserId(uuid)).thenReturn(Optional.of(user));
  }

  @NotNull
  private User stubValidUser() {
    User user = createStandardUser();
    User userWithIdentityProviderId = user.toBuilder()
        .identityProviderUserId(UUID.randomUUID())
        .build();

    when(identityProvider.checkIfUserExists(ANY_EMAIL)).thenReturn(false);
    when(identityProvider.createStandardUser(user)).thenReturn(userWithIdentityProviderId);
    return user;
  }

  private User buildUserFromDB() {
    return User.builder()
        .id(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .build();
  }

  private UserEntity buildUserEntity() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .accountPermissions(Arrays.asList(AccountPermission.builder()
            .name(Permission.MAKE_PAYMENTS)
            .description("Any Description")
            .build()))
        .build();
  }

  private User buildUserFromUserEntity(UserEntity userEntity) {
    List<String> permissions = userEntity.getAccountPermissions().stream()
        .map(accountPermission -> accountPermission.getName().toString())
        .collect(Collectors.toList());
    return User.builder()
        .id(userEntity.getId())
        .identityProviderUserId(userEntity.getIdentityProviderUserId())
        .accountId(userEntity.getAccountId())
        .accountPermissions(permissions)
        .build();
  }

  private User buildUserWithIdentityProviderUser(User user) {
    return user.toBuilder()
        .email(ANY_EMAIL)
        .name("ANY_NAME")
        .build();
  }

  private User createStandardUser() {
    return User.builder()
        .identityProviderUserId(UUID.randomUUID())
        .email(ANY_EMAIL)
        .name("ANY_NAME")
        .build();
  }

  private UserEntity createAdminUser() {
    return UserEntity.builder()
        .identityProviderUserId(UUID.randomUUID())
        .isOwner(true)
        .email(ANY_EMAIL)
        .build();
  }
}