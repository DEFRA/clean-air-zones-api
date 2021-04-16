package uk.gov.caz.accounts.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.correlationid.Constants;

@MockedMvcIntegrationTest
class UsersControllerTestIT {

  private static final String SAMPLE_EMAIL = "example2@email.com";
  private static final String EXISTING_USER_ACCOUNT_ID = "f54554b1-d582-43da-9899-ee33b679e49f";
  private static final String OWNER_USER_ACCOUNT_ID = "4e581c88-3ba3-4df0-91a3-ad46fb48bfd1";
  private static final String TEST_NAME = "Test Name";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private IdentityProvider identityProvider;

  @BeforeEach
  public void setup() {
    executeSqlFrom("data/sql/delete-user-data.sql");
    executeSqlFrom("data/sql/create-account-user.sql");
  }

  @AfterEach
  public void cleanup() {
    executeSqlFrom("data/sql/delete-user-data.sql");
  }

  @Test
  public void shouldFetchExistingUserWithDetailsFromIdentityProvider() throws Exception {
    stubUserInIdentityProvider("08d84742-b196-481e-b2d2-1bb0d7324d0d");

    mockMvc.perform(
        get(UsersController.USERS_PATH + "/" + EXISTING_USER_ACCOUNT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID().toString())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"))
        .andExpect(jsonPath("$.accountUserId").value(EXISTING_USER_ACCOUNT_ID))
        .andExpect(jsonPath("$.accountName").value("test"))
        .andExpect(jsonPath("$.email").value(SAMPLE_EMAIL))
        .andExpect(jsonPath("$.owner").value(false))
        .andExpect(jsonPath("$.name").value(TEST_NAME))
        .andExpect(jsonPath("$.removed").value(false));
  }

  @Test
  public void shouldFetchRemovedUser() throws Exception {
    String removedAccountUserId = "cf89d141-0dfd-4f50-bf29-a0a444279637";

    mockMvc.perform(
        get(UsersController.USERS_PATH + "/" + removedAccountUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID().toString())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"))
        .andExpect(jsonPath("$.accountUserId").value(removedAccountUserId))
        .andExpect(jsonPath("$.owner").value(false))
        .andExpect(jsonPath("$.removed").value(true));
  }

  @Test
  public void shouldFetchOwnerUser() throws Exception {
    stubUserInIdentityProvider("d2b55341-551e-498d-a7be-a6e7f8639161");

    mockMvc.perform(
        get(UsersController.USERS_PATH + "/" + OWNER_USER_ACCOUNT_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID().toString())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value("1f30838f-69ee-4486-95b4-7dfcd5c6c67c"))
        .andExpect(jsonPath("$.accountUserId").value(OWNER_USER_ACCOUNT_ID))
        .andExpect(jsonPath("$.owner").value(true))
        .andExpect(jsonPath("$.removed").value(false));
  }

  @Test
  public void shouldGet404ForNonExistingUser() throws Exception {
    String randomUserId = UUID.randomUUID().toString();

    mockMvc.perform(
        get(UsersController.USERS_PATH + "/" + randomUserId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID().toString())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  private UserEntity createUser(String userId) {
    return UserEntity.builder()
        .email(UsersControllerTestIT.SAMPLE_EMAIL)
        .name(TEST_NAME)
        .identityProviderUserId(UUID.fromString(userId))
        .build();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private UserEntity stubUserInIdentityProvider(String userId) {
    return identityProvider.createStandardUser(createUser(userId));
  }
}