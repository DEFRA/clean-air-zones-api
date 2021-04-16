package uk.gov.caz.accounts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.controller.AccountsController.ACCOUNTS_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.AccountCreationRequestDto;
import uk.gov.caz.accounts.dto.AccountUpdateRequestDto;
import uk.gov.caz.accounts.dto.AccountVerificationRequestDto;
import uk.gov.caz.accounts.dto.CreateAndInviteUserRequestDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.dto.UserValidationRequest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.service.UserService;

@MockedMvcIntegrationTest
public class AccountsControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";
  public static final String EMAIL = "user@mail.com";
  private static final String PASSWORD = "password";
  private static final String VERIFICATION_URL = "http://example.com";
  private static final String EXAMPLE_EMAIL = "example1@email.com";
  private static final String UNVERIFIED_WITH_CODES_EMAIL = "unverified@example.com";
  private static final UUID EXISTING_ACCOUNT_ID = UUID
      .fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");
  private static final UUID EXISTING_USER_ID = UUID
      .fromString("4e581c88-3ba3-4df0-91a3-ad46fb48bfd1");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private IdentityProvider identityProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private UserService userService;

  @Autowired
  private AmazonSQS sqsClient;

  @Value("${services.sqs.new-queue-name}")
  private String emailSqsQueueName;

  @BeforeEach
  public void setup() {
    CreateQueueRequest createQueueRequest = new CreateQueueRequest(emailSqsQueueName)
        .withAttributes(Collections.singletonMap("FifoQueue", "true"));
    sqsClient.createQueue(createQueueRequest);
    executeSqlFrom("data/sql/delete-user-data.sql");
    executeSqlFrom("data/sql/create-account-user.sql");
  }

  @AfterEach
  public void cleanup() {
    GetQueueUrlResult queueUrlResult = sqsClient.getQueueUrl(emailSqsQueueName);
    sqsClient.deleteQueue(queueUrlResult.getQueueUrl());
    executeSqlFrom("data/sql/delete-user-data.sql");
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "caz_account.t_account_user_code");
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "caz_account.t_account_user");
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "caz_account.t_account");
    identityProvider.deleteUser(EMAIL);
  }

  @Test
  public void shouldCreateAccount() throws Exception {
    //given
    AccountCreationRequestDto dto = createAccountRequest();

    //when
    ResultActions callResult = callCreateAccountEndpoint(dto);

    //then
    assertProperResponseSchema(callResult, dto);
    assertAccountIsCreated(dto.getAccountName());
  }

  @Test
  public void shouldUpdateAccountName() throws Exception {
    //given
    AccountUpdateRequestDto dto = createAccountUpdateRequest();

    //when
    ResultActions callResult = callUpdateAccountEndpoint(dto);

    //then
    callResult.andExpect(status().isNoContent());
    Optional<Account> existingAccount = accountRepository.findById(EXISTING_ACCOUNT_ID);
    assertThat(existingAccount).isPresent();
    assertThat(existingAccount.get().getName()).isEqualTo("updated name");
  }

  @Test
  public void shouldFetchExistingAccount() throws Exception {
    //when
    ResultActions callResult = callGetAccount(EXISTING_ACCOUNT_ID);

    //then
    callResult.andExpect(status().isOk())
        .andExpect(jsonPath("$.accountName", is("test")));
  }

  @Test
  public void shouldNotFetchExistingAccount() throws Exception {
    //when
    ResultActions callResult = callGetAccount(UUID.randomUUID());

    //then
    callResult.andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @MethodSource("invalidAccountsPayload")
  public void shouldGetValidationError(AccountCreationRequestDto dto, String expectedErrorMessage)
      throws Exception {
    //when
    ResultActions callResult = callCreateAccountEndpoint(dto);

    //then
    callResult.andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", is(expectedErrorMessage)))
        .andExpect(jsonPath("$.status", is(400)));
  }

  @Nested
  class VerifyUserAccountValidation {

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "not-uuid", "123fdsfa123"})
    public void shouldGetValidationErrorOnInvalidTokenFormat(String token) throws Exception {

      // when
      ResultActions result = callVerifyUserAccountEndpoint(token);

      // then
      result.andExpect(status().isBadRequest());
    }
  }

  @Nested
  class CreateUser {

    @Test
    public void shouldCreateAccountWithPermissions() throws Exception {
      //given
      String email = UUID.randomUUID() + "@mail.com";
      CreateAndInviteUserRequestDto createAndInviteUserRequestDto = CreateAndInviteUserRequestDto
          .builder()
          .email(email)
          .name("sample name")
          .isAdministeredBy(EXISTING_USER_ID.toString())
          .permissions(Collections.singleton(Permission.MAKE_PAYMENTS.toString()))
          .verificationUrl("https://google.com")
          .build();

      //when
      ResultActions resultActions = callInviteUser(EXISTING_ACCOUNT_ID,
          createAndInviteUserRequestDto);

      //then
      UserEntity user = userService.getUserByEmail(email)
          .flatMap(u -> userService.getUserForAccountId(EXISTING_ACCOUNT_ID, u.getId()))
          .orElseThrow(() -> new RuntimeException("cannot find user"));

      resultActions.andExpect(status().isCreated());
      assertThat(user).isNotNull();
      assertThat(user.getAccountPermissions()).hasSize(1);
      assertThat(user.getAccountPermissions().stream().findFirst().get().getName())
          .isEqualTo(Permission.MAKE_PAYMENTS);
    }

    @Test
    public void shouldCreateAccountWithoutPermissions() throws Exception {
      //given
      String email = UUID.randomUUID() + "@mail.com";
      CreateAndInviteUserRequestDto createAndInviteUserRequestDto = CreateAndInviteUserRequestDto
          .builder()
          .email(email)
          .name("sample name")
          .isAdministeredBy(EXISTING_USER_ID.toString())
          .verificationUrl("https://google.com")
          .build();

      //when
      ResultActions resultActions = callInviteUser(EXISTING_ACCOUNT_ID,
          createAndInviteUserRequestDto);

      //then
      UserEntity user = userService.getUserByEmail(email)
          .flatMap(u -> userService.getUserForAccountId(EXISTING_ACCOUNT_ID, u.getId()))
          .orElseThrow(() -> new RuntimeException("cannot find user"));

      resultActions.andExpect(status().isCreated());
      assertThat(user).isNotNull();
      assertThat(user.getAccountPermissions()).isEmpty();
    }
  }

  @Nested
  class EmailValidation {

    @Test
    public void shouldReturnBadRequestIfProvidedEmailIsEmpty() throws Exception {
      //given
      UserValidationRequest userValidationRequest = UserValidationRequest.builder()
          .build();

      //when
      ResultActions result = callValidateUserEndpoint(UUID.randomUUID(), userValidationRequest);

      //then
      result.andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", is(
              "Email cannot be blank"
          )))
          .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    public void shouldReturn204IfUserWithGivenEmailDoesNotExists()
        throws Exception {
      //given
      UserValidationRequest userValidationRequest = UserValidationRequest.builder()
          .email("email@here.com")
          .build();

      //when
      ResultActions result = callValidateUserEndpoint(UUID.randomUUID(), userValidationRequest);

      //then
      result.andExpect(status().is(204));
    }

    @Test
    public void shouldReturn204IfUserWithGivenEmailExistsUnverifiedWithoutCodes()
        throws Exception {
      //given
      stubUnverifiedUsersInIdentityProvider();
      UserValidationRequest userValidationRequest = UserValidationRequest.builder()
          .email(EXAMPLE_EMAIL)
          .build();

      //when
      ResultActions result = callValidateUserEndpoint(UUID.randomUUID(), userValidationRequest);

      //then
      result.andExpect(status().is(204));
    }

    @Test
    public void shouldReturnBadRequestIfActiveUserWithEmailAlreadyExists() throws Exception {
      //given
      stubUsersInIdentityProvider();
      UserValidationRequest userValidationRequest = UserValidationRequest.builder()
          .email(EXAMPLE_EMAIL)
          .build();

      //when
      ResultActions result = callValidateUserEndpoint(EXISTING_ACCOUNT_ID, userValidationRequest);

      //then
      result.andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", is(
              "Provided email is not unique"
          )))
          .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    public void shouldReturnBadRequestIfUnverifiedUserWithEmailAlreadyExistsWithActiveCodes()
        throws Exception {
      //given
      stubUnverifiedUsersInIdentityProvider();
      UserValidationRequest userValidationRequest = UserValidationRequest.builder()
          .email(UNVERIFIED_WITH_CODES_EMAIL)
          .build();

      //when
      ResultActions result = callValidateUserEndpoint(EXISTING_ACCOUNT_ID, userValidationRequest);

      //then
      result.andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", is(
              "Provided email is not unique"
          )))
          .andExpect(jsonPath("$.status", is(400)));
    }
  }

  private ResultActions callGetAccount(UUID accountId) throws Exception {
    return mockMvc.perform(get(ACCOUNTS_PATH + "/{accountId}", accountId)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private ResultActions callInviteUser(UUID accountId,
      CreateAndInviteUserRequestDto createAndInviteUserRequestDto) throws Exception {
    return mockMvc.perform(post(ACCOUNTS_PATH + "/{accountId}/user-invitations", accountId)
        .content(createPayload(createAndInviteUserRequestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private ResultActions callValidateUserEndpoint(UUID accountId,
      UserValidationRequest userValidationRequest) throws Exception {
    return mockMvc.perform(post(ACCOUNTS_PATH + "/{accountId}/user-validations", accountId)
        .content(createPayload(userValidationRequest))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private ResultActions callCreateAccountEndpoint(AccountCreationRequestDto dto) throws Exception {
    return mockMvc.perform(post(ACCOUNTS_PATH)
        .content(createPayload(dto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private ResultActions callUpdateAccountEndpoint(AccountUpdateRequestDto dto) throws Exception {
    return mockMvc.perform(patch(ACCOUNTS_PATH + "/{accountId}", EXISTING_ACCOUNT_ID.toString())
        .content(createPayload(dto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private ResultActions callVerifyUserAccountEndpoint(String token) throws Exception {
    AccountVerificationRequestDto dto = AccountVerificationRequestDto.builder()
        .token(token)
        .build();
    return mockMvc.perform(post(ACCOUNTS_PATH + "/verify")
        .content(createPayload(dto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private void assertProperResponseSchema(ResultActions callResult, AccountCreationRequestDto dto)
      throws Exception {
    callResult
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId", notNullValue()))
        .andExpect(
            header().string(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE))
        .andExpect(
            header().string(PRAGMA_HEADER, PRAGMA_HEADER_VALUE))
        .andExpect(
            header().string(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE))
        .andExpect(
            header().string(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE))
        .andExpect(
            header().string(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE))
        .andExpect(
            header().string(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE));
    boolean accountExists = JdbcTestUtils
        .countRowsInTableWhere(jdbcTemplate, "caz_account.t_account",
            String.format("account_name = '%s'", dto.getAccountName())) == 1;
    assertThat(accountExists).isTrue();
  }

  @SneakyThrows
  private String createPayload(Object o) {
    return objectMapper.writeValueAsString(o);
  }

  static Stream<Arguments> invalidAccountsPayload() {
    AccountCreationRequestDto validAccountPayload = createAccountRequest();
    return Stream.of(
        arguments(validAccountPayload.toBuilder().accountName(null).build(),
            "Account name cannot be null."),
        arguments(validAccountPayload.toBuilder().accountName("").build(),
            "Account name cannot be empty."),
        arguments(validAccountPayload.toBuilder()
            .accountName(IntStream.rangeClosed(0, 101).mapToObj(Integer::toString).collect(
                Collectors.joining(""))).build(), "Account name is too long."),
        arguments(validAccountPayload.toBuilder().accountName("L??~=###:;%$").build(),
            "Account name cannot include invalid characters.")
    );
  }

  private void assertAccountIsCreated(String accountName) {
    Iterable<Account> accounts = accountRepository.findAll();
    assertThat(accounts).isNotEmpty();

    Account account = StreamSupport.stream(accounts.spliterator(), false)
        .filter(e -> e.getName().equals(accountName))
        .findFirst().get();
    assertThat(account.getId()).isNotNull();
    assertThat(account.getName()).isEqualTo(accountName);
  }

  public static UserForAccountCreationRequestDto createUserRequest() {
    return UserForAccountCreationRequestDto.builder()
        .password(PASSWORD)
        .email(EMAIL)
        .verificationUrl(VERIFICATION_URL)
        .build();
  }

  public static AccountCreationRequestDto createAccountRequest() {
    return AccountCreationRequestDto.builder()
        .accountName(UUID.randomUUID().toString())
        .build();
  }

  public static AccountUpdateRequestDto createAccountUpdateRequest() {
    return AccountUpdateRequestDto.builder()
        .accountName("updated name")
        .build();
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private void stubUsersInIdentityProvider() {
    Stream.of(
        createUser("54f04990-fea5-4ca2-9c60-834a5d9ba411", EXAMPLE_EMAIL, true),
        createUser("08d84742-b196-481e-b2d2-1bb0d7324d0d", "example2@email.com", true)
    ).forEach(identityProvider::createStandardUser);
  }

  private void stubUnverifiedUsersInIdentityProvider() {
    Stream.of(
        createUser("54f04990-fea5-4ca2-9c60-834a5d9ba411", EXAMPLE_EMAIL, false),
        createUser("5d5e79d8-6055-4d0f-a841-829b97b79279", UNVERIFIED_WITH_CODES_EMAIL, false)
    ).forEach(identityProvider::createStandardUser);
  }

  private UserEntity createUser(String identityProviderUserId, String email,
      boolean emailVerified) {
    return UserEntity.builder()
        .email(email)
        .name("Test Name")
        .emailVerified(emailVerified)
        .identityProviderUserId(UUID.fromString(identityProviderUserId))
        .build();
  }
}
