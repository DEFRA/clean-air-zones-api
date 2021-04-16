package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.InvitingUserNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountStandardUserCreatorServiceTest {

  private static final UUID INVALID_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ACCOUNT_ID = UUID.fromString("497c84f4-5062-40f2-a306-dd922f08d82c");
  private static final UUID ADMIN_ACCOUNT_USER_ID = UUID
      .fromString("46000be4-6572-11eb-ae93-0242ac130002");
  private static final UUID INVALID_ADMIN_ACCOUNT_USER_ID = UUID.randomUUID();
  private static final UUID ACCOUNT_USER_ID = UUID
      .fromString("61cf598c-6564-4c8d-aced-7e2857a64522");
  private static final String ACCOUNT_USER_NAME = "TestName";
  private static final Set<Permission> ANY_PERMISSIONS = Collections
      .singleton(Permission.MAKE_PAYMENTS);

  private static final String EMAIL = "dev@jaqu.gov";
  private static final URI VERIFICATION_URL = URI.create("http://example.com");

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AccountUpdateService accountUpdateService;

  @Mock
  private DuplicatedAccountUserService duplicatedAccountUserService;

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private PasswordResetService passwordResetService;

  @Mock
  private UserPermissionsService userPermissionsService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserService userService;

  @InjectMocks
  private AccountStandardUserCreatorService accountStandardUserCreatorService;

  @Nested
  class InputValidation {

    @Test
    public void shouldThrowNullPointerExceptionWhenAccountIdIsNull() {
      Throwable throwable = catchThrowable(() -> accountStandardUserCreatorService
          .createStandardUserForAccount(null, ADMIN_ACCOUNT_USER_ID, EMAIL, ACCOUNT_USER_NAME,
              ANY_PERMISSIONS, VERIFICATION_URL));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("accountId cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenEmailIsNull() {
      Throwable throwable = catchThrowable(() -> accountStandardUserCreatorService
          .createStandardUserForAccount(ACCOUNT_ID, ADMIN_ACCOUNT_USER_ID, null, ACCOUNT_USER_NAME,
              ANY_PERMISSIONS, VERIFICATION_URL));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("email cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenVerificationUriIsNull() {
      Throwable throwable = catchThrowable(() -> accountStandardUserCreatorService
          .createStandardUserForAccount(ACCOUNT_ID, ADMIN_ACCOUNT_USER_ID, EMAIL, ACCOUNT_USER_NAME,
              ANY_PERMISSIONS, null));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("verificationUri cannot be null");
    }

    @Test
    public void shouldThrowAccountNotFoundExceptionAccountIsNotPresentInDB() {
      Throwable throwable = catchThrowable(() -> accountStandardUserCreatorService
          .createStandardUserForAccount(INVALID_ACCOUNT_ID, ADMIN_ACCOUNT_USER_ID, EMAIL,
              ACCOUNT_USER_NAME, ANY_PERMISSIONS, VERIFICATION_URL));
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    @Test
    public void shouldThrowInvitingUserNotFoundExceptionAccountIsNotPresentInDB() {
      mockAccountFound();

      Throwable throwable = catchThrowable(() -> accountStandardUserCreatorService
          .createStandardUserForAccount(ACCOUNT_ID, INVALID_ADMIN_ACCOUNT_USER_ID, EMAIL,
              ACCOUNT_USER_NAME, ANY_PERMISSIONS, VERIFICATION_URL));
      assertThat(throwable).isInstanceOf(InvitingUserNotFoundException.class)
          .hasMessage("The user who initiated the invitation was not found");
    }
  }

  @Nested
  class NewUserCreationScenario {

    @Test
    public void shouldCallEmailIssuerOnSuccessfulUserCreation() {
      // given
      mockSuccessfulAccountUserCreation();

      // when
      callCreationService();

      // then
      verify(passwordResetService)
          .generateAndSaveResetTokenForInvitedUser(any(), any(), any());
    }

    @Test
    public void shouldNotCallDuplicatedAccountUserService() {
      mockSuccessfulAccountUserCreation();
      callCreationService();
      verify(duplicatedAccountUserService, never()).resolveAccountUserDuplication(any());
    }
  }

  @Nested
  class ExistingUserAlterationScenario {

    @Test
    public void shouldCallEmailIssuerOnSuccessfulUserCreation() {
      // given
      mockSuccessfulAccountUserAlteration();

      // when
      callCreationService();

      // then
      verify(passwordResetService)
          .generateAndSaveResetTokenForInvitedUser(any(), any(), any());
    }

    @Test
    public void shouldResolveAccountUsersDuplication() {
      // given
      mockSuccessfulAccountUserAlteration();

      // when
      callCreationService();

      // then
      verify(duplicatedAccountUserService).resolveAccountUserDuplication(any());
      verify(userService).createStandardUserForExistingEmail(any(), any(), any(), any());
      verify(identityProvider).setUserName(EMAIL, ACCOUNT_USER_NAME);
    }
  }

  private void mockAccountFound() {
    Account account = Account.builder().name("AnyAccountName").build();

    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
  }

  private void mockSuccessfulAccountUserCreation() {
    mockAccountFound();
    mockAdminUserFound();
    mockSuccessfulUserCreation();
    mockSuccessfulTokenGeneration();
  }

  private void mockSuccessfulAccountUserAlteration() {
    mockAccountFound();
    mockAdminUserFound();
    mockSuccessfulUserAlteration();
    mockSuccessfulTokenGeneration();
  }

  private void mockSuccessfulUserAlteration() {
    UserEntity user = sampleCreatedUser();
    when(userService.getUserEntityByEmail(any())).thenReturn(Optional.of(user));
    doNothing().when(duplicatedAccountUserService).resolveAccountUserDuplication(any());
    when(userService.createStandardUserForExistingEmail(any(), any(), any(), any()))
        .thenReturn(user);
  }

  private void mockSuccessfulTokenGeneration() {
    doNothing().when(passwordResetService)
        .generateAndSaveResetTokenForInvitedUser(any(), any(), any());
  }

  private void mockSuccessfulUserCreation() {
    UserEntity user = sampleCreatedUser();
    when(userService.createStandardUser(any())).thenReturn(user);
  }

  private void mockAdminUserFound() {
    UserEntity user = UserEntity.builder().isOwner(true).id(ADMIN_ACCOUNT_USER_ID).build();
    when(userRepository.findById(ADMIN_ACCOUNT_USER_ID)).thenReturn(Optional.of(user));
  }

  private UserEntity sampleCreatedUser() {
    return UserEntity.builder()
        .id(ACCOUNT_USER_ID)
        .email(EMAIL)
        .identityProviderUserId(UUID.randomUUID())
        .accountId(ACCOUNT_ID)
        .isOwner(false)
        .emailVerified(true)
        .build();
  }

  private void callCreationService() {
    accountStandardUserCreatorService.createStandardUserForAccount(
        ACCOUNT_ID,
        ADMIN_ACCOUNT_USER_ID,
        EMAIL,
        ACCOUNT_USER_NAME,
        ANY_PERMISSIONS,
        VERIFICATION_URL
    );
  }
}