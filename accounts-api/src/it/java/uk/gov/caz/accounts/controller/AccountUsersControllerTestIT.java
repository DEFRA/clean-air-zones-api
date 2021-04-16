package uk.gov.caz.accounts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.controller.AccountUsersController.USERS_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.UpdateUserRequestDto;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;

@MockedMvcIntegrationTest
class AccountUsersControllerTestIT {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AccountUserRepository accountUserRepository;

  @Autowired
  private IdentityProvider identityProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final UUID EXISTING_ACCOUNT_ID = UUID
      .fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");
  private static final UUID NON_EXISTING_ACCOUNT_ID = UUID
      .fromString("b6968560-cb56-4248-9f8f-d75b0aff726e");
  private static final UUID EXISTING_ACCOUNT_USER_ID = UUID
      .fromString("49401c98-2141-4cf4-8cec-2ab9635806a9");

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  @BeforeEach
  public void setUpDb() {
    executeSqlFrom("data/sql/delete-user-data.sql");
    executeSqlFrom("data/sql/create-account-user.sql");
  }

  @AfterEach
  public void cleanUpDb() {
    executeSqlFrom("data/sql/delete-user-data.sql");
  }

  @Nested
  class GetUsers {

    @Test
    public void shouldReturn200WhenAccountAndUsersExistsInDB() throws Exception {
      stubUsersInIdentityProvider();
      performGetRequestForAccount(EXISTING_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.users").exists())
          .andExpect(jsonPath("$.users").isNotEmpty())
          .andExpect(jsonPath("$.multiPayerAccount").value(false));
    }

    @Test
    public void shouldReturn404WhenAccountIdDoesNotExist() throws Exception {
      performGetRequestForAccount(NON_EXISTING_ACCOUNT_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    private ResultActions performGetRequestForAccount(UUID accountId)
        throws Exception {
      return mockMvc.perform(get(USERS_PATH, accountId)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class DeleteUser {

    @Test
    void shouldDeleteUser() throws Exception {
      UUID accountUserIdForExistingUser = UUID.fromString("f54554b1-d582-43da-9899-ee33b679e49f");

      identityProvider.createStandardUser(UserEntity.builder()
          .id(accountUserIdForExistingUser)
          .email("someemailhere")
          .name("name")
          .accountId(UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"))
          .identityProviderUserId(UUID.fromString("08d84742-b196-481e-b2d2-1bb0d7324d0d"))
          .build());

      deleteUser(accountUserIdForExistingUser).andExpect(status().is(204));
      assertUserIsDeleted(accountUserIdForExistingUser);
    }

    @Test
    void shouldDeleteUserTwiceAndGetTheSameResults() throws Exception {
      UUID accountUserIdForExistingUser = UUID.fromString("f54554b1-d582-43da-9899-ee33b679e49f");

      identityProvider.createStandardUser(UserEntity.builder()
          .id(accountUserIdForExistingUser)
          .email("someemailhere")
          .name("name")
          .accountId(UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"))
          .identityProviderUserId(UUID.fromString("08d84742-b196-481e-b2d2-1bb0d7324d0d"))
          .build());

      deleteUser(accountUserIdForExistingUser).andExpect(status().is(204));
      deleteUser(accountUserIdForExistingUser).andExpect(status().is(204));

      assertUserIsDeleted(accountUserIdForExistingUser);
    }

    @Test
    void shouldNotDeleteOwnerUser() throws Exception {
      UUID accountUserIdForExistingUser = UUID.fromString("c11826f3-3ec7-4bd2-9b26-3653bb46c889");

      deleteUser(accountUserIdForExistingUser).andExpect(status().is(403));
      Optional<User> optionalUser = accountUserRepository.findById(accountUserIdForExistingUser);
      assertThat(optionalUser).isPresent();
      assertThat(optionalUser.get().getIdentityProviderUserId())
          .isEqualByComparingTo(UUID.fromString("1b93e6e3-cff8-45b9-bcda-f7245defaeb5"));
    }

    @Test
    void shouldNotDeleteNotExistingUser() throws Exception {
      UUID accountUserId = UUID.randomUUID();

      deleteUser(accountUserId).andExpect(status().is(422));
    }

    @NotNull
    private ResultActions deleteUser(UUID accountUserIdForExistingUser) throws Exception {
      return performRequestForUserDeletion(accountUserIdForExistingUser)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
    }

    private void assertUserIsDeleted(UUID accountUserIdForExistingUser) {
      Optional<User> optionalUser = accountUserRepository.findById(accountUserIdForExistingUser);
      assertThat(optionalUser).isPresent();
      assertThat(optionalUser.get().getIdentityProviderUserId()).isNull();
    }

    private ResultActions performRequestForUserDeletion(UUID accountUserId)
        throws Exception {
      return mockMvc
          .perform(delete(USERS_PATH + "/{accountUserId}", EXISTING_ACCOUNT_ID, accountUserId)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class GetUser {

    @Test
    public void shouldReturn200WhenAccountAndUserExistsInDB() throws Exception {
      stubUsersInIdentityProvider();
      performGetRequestForAccountAndUser(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").exists())
          .andExpect(jsonPath("$.name").exists());
    }

    @Test
    public void shouldReturn404WhenAccountIdDoesNotExist() throws Exception {
      performGetRequestForAccountAndUser(NON_EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }}

  @Test
  public void shouldReturn404WhenUserIsDeleted() throws Exception {
    //given
    UUID userId = userIsDeleted();

    //then
    performGetRequestForAccountAndUser(EXISTING_ACCOUNT_ID, userId)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isNotFound());
  }

  private UUID userIsDeleted() throws Exception {
    UUID accountUserIdForExistingUser = UUID.fromString("f54554b1-d582-43da-9899-ee33b679e49f");

    identityProvider.createStandardUser(UserEntity.builder()
        .id(accountUserIdForExistingUser)
        .email("someemailhere")
        .name("name")
        .accountId(EXISTING_ACCOUNT_ID)
        .identityProviderUserId(UUID.fromString("08d84742-b196-481e-b2d2-1bb0d7324d0d"))
        .build());

    deleteUser(accountUserIdForExistingUser).andExpect(status().is(204));

    return accountUserIdForExistingUser;
  }

  @NotNull
  private ResultActions deleteUser(UUID accountUserIdForExistingUser) throws Exception {
    return performRequestForUserDeletion(accountUserIdForExistingUser)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
  }

  private ResultActions performRequestForUserDeletion(UUID accountUserId)
      throws Exception {
    return mockMvc
        .perform(delete(USERS_PATH + "/{accountUserId}", EXISTING_ACCOUNT_ID, accountUserId)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions performGetRequestForAccountAndUser(UUID accountId, UUID accountUserId)
        throws Exception {
    return mockMvc.perform(get(USERS_PATH + "/{accountUserId}", accountId, accountUserId)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .accept(MediaType.APPLICATION_JSON));
  }

  private void stubUsersInIdentityProvider() {
    Stream.of(
        createUser("54f04990-fea5-4ca2-9c60-834a5d9ba411", "example1@email.com"),
        createUser("08d84742-b196-481e-b2d2-1bb0d7324d0d", "example2@email.com"),
        createUser("5d5e79d8-6055-4d0f-a841-829b97b79279", "example3@email.com"),
        createUser("d2b55341-551e-498d-a7be-a6e7f8639161", "owner1@email.com"),
        createUser("1b93e6e3-cff8-45b9-bcda-f7245defaeb5", "owner2@email.com")
    ).forEach(identityProvider::createStandardUser);
  }

  private UserEntity createUser(String identityProviderUserId, String email) {
    return UserEntity.builder()
        .email(email)
        .name("Test Name")
        .identityProviderUserId(UUID.fromString(identityProviderUserId))
        .build();
  }

  @Nested
  class UpdateUserName {

    @Test
    public void shouldUpdateUserNameForUserWhichExists() throws Exception {
      String accountId = "1f30838f-69ee-4486-95b4-7dfcd5c6c67c";
      String email = "someemailhere";
      String accountUserId = "49401c98-2141-4cf4-8cec-2ab9635806a9";
      String identityProviderId = "54f04990-fea5-4ca2-9c60-834a5d9ba411";

      UserEntity user = createUser(identityProviderId, email);
      identityProvider.createStandardUser(user);
      String oldUsername = user.getName();

      String newUsername = oldUsername + RandomStringUtils.randomAlphabetic(2);
      performRequestWithNewName(
          UUID.fromString(accountId), UUID.fromString(accountUserId), newUsername)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      assertThat(identityProvider.getUser(email).getName()).isEqualTo(newUsername);
    }

    @Test
    public void shouldNotUpdateUserWhenHeDoesNotExist() throws Exception {
      String accountId = "1f30838f-69ee-4486-95b4-7dfcd5c6c67c";
      String email = "someemailhere";
      String nonExistingAccountUserId = "088063af-5e19-4266-9aae-c7a2226139da";
      String identityProviderId = "54f04990-fea5-4ca2-9c60-834a5d9ba411";

      UserEntity user = createUser(identityProviderId, email);
      identityProvider.createStandardUser(user);
      String oldUsername = user.getName();

      String newUsername = oldUsername + RandomStringUtils.randomAlphabetic(2);
      performRequestWithNewName(
          UUID.fromString(accountId), UUID.fromString(nonExistingAccountUserId), newUsername)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());

      assertThat(identityProvider.getUser(email).getName()).isEqualTo(oldUsername);
    }

    private ResultActions performRequestWithNewName(UUID accountId, UUID accountUserId,
        String newName) throws Exception {

      return mockMvc.perform(patch(USERS_PATH + "/{accountUserId}", accountId, accountUserId)
          .content(toJson(toRequest(null, newName)))
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class UpdateUserPermissions {
    @ParameterizedTest
    @MethodSource("uk.gov.caz.accounts.controller.AccountUsersControllerTestIT#singlePermission")
    public void shouldUpdateAccountPermissionsToSinglePermission(Permission newPermission) throws Exception {
      performRequestWithPermissions(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, Collections.singletonList(newPermission))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      int allPermissionsUserCount = getAllPermissionCountForUser(EXISTING_ACCOUNT_USER_ID);
      int permissionsUserCount = getPermissionCountForUser(newPermission, EXISTING_ACCOUNT_USER_ID);
      assertThat(permissionsUserCount).isOne();
      assertThat(allPermissionsUserCount).isOne();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.accounts.controller.AccountUsersControllerTestIT#twoPermissions")
    public void shouldUpdateAccountPermissionsToTwoPermissions(Permission firstPermission,
        Permission secondPermission) throws Exception {
      List<Permission> permissions = Arrays.asList(firstPermission, secondPermission);
      performRequestWithPermissions(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, permissions)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      int permissionsUserCount = getPermissionCountForUser(permissions, EXISTING_ACCOUNT_USER_ID);
      int allPermissionsUserCount = getAllPermissionCountForUser(EXISTING_ACCOUNT_USER_ID);
      assertThat(permissionsUserCount).isEqualTo(2);
      assertThat(allPermissionsUserCount).isEqualTo(2);
    }

    @Test
    public void shouldRevokeAndGrantAllPermissions() throws Exception {
      performRequestWithPermissions(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, Collections.emptyList())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      int allPermissionsUserCount = getAllPermissionCountForUser(EXISTING_ACCOUNT_USER_ID);
      assertThat(allPermissionsUserCount).isZero();

      EnumSet<Permission> permissions = EnumSet.allOf(Permission.class);
      performRequestWithPermissions(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, permissions)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      allPermissionsUserCount = getAllPermissionCountForUser(EXISTING_ACCOUNT_USER_ID);
      assertThat(permissions).hasSize(allPermissionsUserCount);
    }

    @Test
    public void shouldHandleDuplicatedPermissions() throws Exception {
      Collection<Permission> permissions = ImmutableList.of(Permission.MAKE_PAYMENTS,
          Permission.MAKE_PAYMENTS);
      performRequestWithPermissions(EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, permissions)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());

      int permissionsUserCount = getPermissionCountForUser(
          Collections.singletonList(Permission.MAKE_PAYMENTS),
          EXISTING_ACCOUNT_USER_ID
      );
      int allPermissionsUserCount = getAllPermissionCountForUser(EXISTING_ACCOUNT_USER_ID);
      assertThat(permissionsUserCount).isOne();
      assertThat(allPermissionsUserCount).isOne();
    }

    @Test
    public void shouldReturn404ForAbsentAccount() throws Exception {
      performRequestWithPermissions(NON_EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, Collections.emptyList())
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn400ForNullPermissions() throws Exception {
      performRequestWithPermissions(NON_EXISTING_ACCOUNT_ID, EXISTING_ACCOUNT_USER_ID, null)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest());
    }

    private int getPermissionCountForUser(Permission permission, UUID accountUserId) {
      return getPermissionCountForUser(Collections.singletonList(permission), accountUserId);
    }

    private int getPermissionCountForUser(Collection<Permission> permissions, UUID accountUserId) {
      return JdbcTestUtils.countRowsInTableWhere(
          jdbcTemplate,
          "caz_account.t_account_user_permission aup, caz_account.t_account_permission ap",
          "aup.account_user_id = '" + accountUserId + "'"
              + "AND aup.account_permission_id = ap.account_permission_id "
              + "AND ap.name IN (" + joinWithCommas(permissions) + ")"
      );
    }

    private int getAllPermissionCountForUser(UUID accountUserId) {
      return JdbcTestUtils.countRowsInTableWhere(
          jdbcTemplate,
          "caz_account.t_account_user_permission aup, caz_account.t_account_permission ap",
          "aup.account_user_id = '" + accountUserId + "'"
              + "AND aup.account_permission_id = ap.account_permission_id"
      );
    }

    private ResultActions performRequestWithPermissions(UUID accountId, UUID accountUserId,
        Collection<Permission> permissions) throws Exception {

      return mockMvc.perform(patch(USERS_PATH + "/{accountUserId}", accountId, accountUserId)
          .content(toJson(toRequest(permissions, null)))
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String joinWithCommas(Collection<Permission> collection) {
      return Joiner.on(',').join(
          collection.stream()
              .map(Permission::name)
              .map(object -> "'" + object + "'")
              .collect(Collectors.toList())
      );
    }
  }

  static Stream<Permission> singlePermission() {
    return EnumSet.allOf(Permission.class).stream();
  }

  static Stream<Arguments> twoPermissions() {
    return Stream.of(
        Arguments.arguments(Permission.MAKE_PAYMENTS, Permission.MANAGE_USERS),
        Arguments.arguments(Permission.MANAGE_USERS, Permission.MANAGE_MANDATES),
        Arguments.arguments(Permission.VIEW_PAYMENTS, Permission.MANAGE_VEHICLES),
        Arguments.arguments(Permission.VIEW_PAYMENTS, Permission.MANAGE_MANDATES)
    );
  }

  private UpdateUserRequestDto toRequest(Collection<Permission> permissions, String newName) {
    return UpdateUserRequestDto.builder()
        .permissions(permissions == null
            ? null
            : permissions.stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
        .name(newName)
        .build();
  }

  @SneakyThrows
  private String toJson(Object o) {
    return objectMapper.writeValueAsString(o);
  }

}