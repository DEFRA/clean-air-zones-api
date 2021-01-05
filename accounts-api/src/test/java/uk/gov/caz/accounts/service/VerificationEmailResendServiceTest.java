package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.InvalidActiveVerificationCodesAmount;

@ExtendWith(MockitoExtension.class)
class VerificationEmailResendServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  private VerificationEmailIssuerService verificationEmailIssuerService;

  @InjectMocks
  private VerificationEmailResendService verificationEmailResendService;

  private static final String ACCOUNT_USER_ID = "eb01a509-3a1c-456c-924f-a2c14ac149af";
  private static final String ACCOUNT_ID = "11c10b17-197c-49ab-abd4-011ab898631c";
  private static final String ANY_EMAIL = "sample@email.com";

  private static final URI ANY_VERIFICATION_URI = URI.create("http://example.com");
  private static final LocalDateTime ANY_EXPIRATION_DATE = LocalDateTime.now().plusDays(1);

  @Nested
  class InputValidation {

    @Test
    public void shouldThrowExceptionWhenAccountIdIsNull() {
      Throwable throwable = catchThrowable(() -> verificationEmailResendService
          .resendVerificationEmail(null, UUID.fromString(ACCOUNT_USER_ID), ANY_VERIFICATION_URI));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("accountId cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenAccountUserIdIsNull() {
      Throwable throwable = catchThrowable(() -> verificationEmailResendService
          .resendVerificationEmail(UUID.fromString(ACCOUNT_ID), null, ANY_VERIFICATION_URI));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("accountUserId cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenVerificationUriIsNull() {
      Throwable throwable = catchThrowable(() -> verificationEmailResendService
          .resendVerificationEmail(UUID.fromString(ACCOUNT_ID), UUID.fromString(ACCOUNT_USER_ID),
              null));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("verificationUri cannot be null");
    }

    @Test
    public void shouldThrowExceptionWhenAccountWithProvidedIdDoesNotExist() {
      mockMissingAccountId();

      Throwable throwable = catchThrowable(() -> callVerificationEmailResendService());
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    private void mockMissingAccountId() {
      when(accountRepository.findById(any())).thenReturn(Optional.empty());
    }
  }

  @Test
  public void shouldThrowExceptionWhenUserHasWrongAmountOfActiveCodes() {
    // given
    mockFoundAccount();
    mockFoundUserInDatabase();
    mockMultipleActiveVerificationCodes();

    // when
    Throwable throwable = catchThrowable(() -> callVerificationEmailResendService());

    // then
    assertThat(throwable).isInstanceOf(InvalidActiveVerificationCodesAmount.class);
  }

  @Test
  public void shouldThrowExceptionWhenUserIsNotFound() {
    // given
    mockFoundAccount();
    mockMissingUser();

    // when
    Throwable throwable = catchThrowable(() -> callVerificationEmailResendService());

    // then
    assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
        .hasMessage("AccountUser was not found.");
  }

  @Test
  public void shouldUpdateActiveUserCodeAndCallEmailSendingProcess() {
    // given
    mockFoundAccount();
    mockSingleActiveVerificationCode();
    mockFoundUserInDatabase();
    mockFoundUserInThirdPartyService();

    // when
    callVerificationEmailResendService();

    // then
    verify(accountUserCodeRepository).save(any());
    verify(verificationEmailIssuerService)
        .generateVerificationTokenAndSendVerificationEmail(any(), any(), any());
  }

  private UserEntity callVerificationEmailResendService() {
    return verificationEmailResendService.resendVerificationEmail(
        UUID.fromString(ACCOUNT_ID),
        UUID.fromString(ACCOUNT_USER_ID),
        ANY_VERIFICATION_URI
    );
  }

  private void mockFoundUserInDatabase() {
    when(userRepository.findById(any())).thenReturn(Optional.of(getSampleUser()));
  }

  private void mockFoundUserInThirdPartyService() {
    when(userService.getCompleteUserDetailsAsUserEntityForAccountUserId(any())).thenReturn(getSampleUser());
  }

  private void mockMissingUser() {
    when(userRepository.findById(any())).thenReturn(Optional.empty());
  }

  private void mockFoundAccount() {
    when(accountRepository.findById(any())).thenReturn(Optional.of(getSampleAccount()));
  }

  private void mockSingleActiveVerificationCode() {
    when(accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeType(
            any(),
            eq(CodeStatus.ACTIVE),
            eq(CodeType.USER_VERIFICATION)))
        .thenReturn(Arrays.asList(getActiveUserCode()));
  }

  private void mockMultipleActiveVerificationCodes() {
    when(accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeType(
            any(),
            eq(CodeStatus.ACTIVE),
            eq(CodeType.USER_VERIFICATION)))
        .thenReturn(Arrays.asList(getActiveUserCode(), getActiveUserCode()));
  }

  private Account getSampleAccount() {
    return Account.builder()
        .id(UUID.fromString(ACCOUNT_ID))
        .name("some-account-name")
        .build();
  }

  private UserEntity getSampleUser() {
    return UserEntity.builder()
        .id(UUID.fromString(ACCOUNT_USER_ID))
        .identityProviderUserId(UUID.randomUUID())
        .accountId(UUID.fromString(ACCOUNT_ID))
        .email(ANY_EMAIL)
        .isOwner(true)
        .emailVerified(true)
        .build();
  }

  private AccountUserCode getActiveUserCode() {
    return AccountUserCode.builder()
        .id(RandomUtils.nextInt())
        .accountUserId(UUID.fromString(ACCOUNT_USER_ID))
        .status(CodeStatus.ACTIVE)
        .codeType(CodeType.USER_VERIFICATION)
        .code("some-code")
        .expiration(ANY_EXPIRATION_DATE)
        .build();
  }
}