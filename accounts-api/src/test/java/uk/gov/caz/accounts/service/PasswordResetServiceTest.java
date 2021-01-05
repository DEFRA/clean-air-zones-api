package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.emailnotifications.PasswordResetEmailSender;
import uk.gov.caz.accounts.service.emailnotifications.UserInvitationEmailSender;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  private TokenToHashConverter tokenToHashConverter;

  @Mock
  private TokensExpiryDatesProvider tokensExpiryDatesProvider;

  @Mock
  private PasswordResetEmailSender passwordResetEmailSender;

  @Mock
  private UserInvitationEmailSender userInvitationEmailSender;

  @InjectMocks
  private PasswordResetService passwordResetService;

  private static final String ANY_EMAIL = "example@email.com";
  private static final URI ANY_URL = URI.create("https://dev.caz.uk/password/reset");

  @Nested
  class PasswordResetForNonExistingUser {

    @Test
    public void shouldNotThrowException() {
      mockMissingUser();

      Throwable throwable = catchThrowable(
          () -> passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL));

      assertThat(throwable).isNull();
    }

    @Test
    public void shouldNotSaveAccountUserCode() {
      mockMissingUser();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository, never()).save(any());
    }

    private void mockMissingUser() {
      when(userService.getUserByEmail(ANY_EMAIL)).thenReturn(Optional.empty());
    }
  }

  @Nested
  class PasswordResetForExistingUser {

    @Test
    public void shouldGenerateHashForPasswordResetToken() {
      mockExistingUser();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(tokenToHashConverter).convert(any());
    }

    @Test
    public void shouldSendPasswordResetEmail() {
      mockExistingUser();
      mockSuccessfulEmailSend();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository).save(any());
      verify(passwordResetEmailSender).send(anyString(), any());
    }

    @Test
    public void shouldSaveAccountUserCodeWhenSendingEmailIsSuccessful() {
      mockExistingUser();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository).save(any());
    }

    @Test
    public void shouldNotSendEmailWhenSavingAccountUserCodeFails() {
      mockExistingUser();
      mockFailureOfSavingToken();

      Throwable throwable = catchThrowable(
          () -> passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL));

      assertThat(throwable).isInstanceOf(RuntimeException.class);
      verify(passwordResetEmailSender, never()).send(any(), any());
    }

    @Test
    public void shouldNotSendEmailWhenUserHas5OrMoreActiveTokensFromLastHour() {
      when(userService.getUserByEmail(any())).thenReturn(mockedUser());
      mockEmailsLimitExceeded();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository, never()).save(any());
    }

    @Test
    public void shouldNotSendEmailWhenUserIsDuringEmailChangeProcess() {
      when(userService.getUserByEmail(any())).thenReturn(mockedUser());
      mockActiveEmailChangeToken();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository, never()).save(any());
    }

    private void mockActiveEmailChangeToken() {
      AccountUserCode code = AccountUserCode.builder()
          .accountUserId(UUID.randomUUID())
          .code("SOME_CODE")
          .status(CodeStatus.ACTIVE)
          .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
          .expiration(LocalDateTime.now().plusHours(1))
          .build();
      when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(any(), any(), any()))
          .thenReturn(Arrays.asList(code));
    }

    @Test
    public void shouldSendPasswordResetEmailWhenUserHas5TokensButAtLeastOneIsUsed() {
      mockExistingUser();
      mockSuccessfulEmailSend();
      mockEmailsLimitNotExceededBecauseOfUsedToken();

      passwordResetService.generateAndSaveResetToken(ANY_EMAIL, ANY_URL);

      verify(accountUserCodeRepository).save(any());
      verify(passwordResetEmailSender).send(anyString(), any());
    }

    private void mockExistingUser() {
      when(userService.getUserByEmail(any())).thenReturn(mockedUser());
      when(tokenToHashConverter.convert(any())).thenReturn("CONVERTED-CODE");
      when(tokensExpiryDatesProvider.getResetTokenExpiryDateFromNow())
          .thenReturn(currentTime().plusDays(1));
    }

    private void mockEmailsLimitExceeded() {
      when(accountUserCodeRepository
          .findByAccountUserIdFromLastHourWithLimit(any(), any(), anyInt()))
          .thenReturn(Collections.nCopies(5, sampleAccountUserCode(CodeStatus.ACTIVE)));
    }

    private void mockEmailsLimitNotExceededBecauseOfUsedToken() {
      List<AccountUserCode> activeCodes = Collections
          .nCopies(4, sampleAccountUserCode(CodeStatus.ACTIVE));
      List<AccountUserCode> usedCodes = Arrays.asList(sampleAccountUserCode(CodeStatus.USED));
      List<AccountUserCode> combinedCodes = Stream.of(activeCodes, usedCodes)
          .flatMap(Collection::stream).collect(Collectors.toList());

      when(accountUserCodeRepository
          .findByAccountUserIdFromLastHourWithLimit(any(), any(), anyInt()))
          .thenReturn(combinedCodes);
    }

    private void mockSuccessfulEmailSend() {
      doNothing().when(passwordResetEmailSender).send(any(), any());
    }

    private void mockFailureOfSavingToken() {
      doThrow(RuntimeException.class)
          .when(accountUserCodeRepository).save(any());
    }

    private Optional<User> mockedUser() {
      User user = User.builder()
          .id(UUID.randomUUID())
          .accountId(UUID.randomUUID())
          .email(ANY_EMAIL)
          .identityProviderUserId(UUID.randomUUID())
          .name("Jan Kowalski")
          .build();

      return Optional.of(user);
    }

    private AccountUserCode sampleAccountUserCode(CodeStatus codeStatus) {
      return AccountUserCode.builder()
          .id(RandomUtils.nextInt())
          .expiration(LocalDateTime.now().plusDays(1))
          .code("SOME_CODE")
          .accountUserId(UUID.randomUUID())
          .status(codeStatus)
          .codeType(CodeType.PASSWORD_RESET)
          .build();
    }

    private LocalDateTime currentTime() {
      return LocalDateTime.now();
    }
  }

  @Nested
  class VerificationTokenForInvitedUser {

    private final URI ANY_VERIFICATION_LINK = URI.create("http://localhost");
    private final Account ANY_ACCOUNT = Account.builder().id(UUID.randomUUID()).build();


    @Nested
    class Validation {

      @Test
      public void shouldThrowNullPointerExceptionIfUserIsNull() {
        // given
        User user = null;

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("user cannot be null");
      }

      @Test
      public void shouldThrowNullPointerExceptionIfVerificationLinkIsNull() {
        // given
        URI verificationLink = null;
        User user = User.builder()
            .isOwner(false)
            .build();

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, verificationLink));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("verificationLink cannot be null");
      }

      @Test
      public void shouldThrowIllegalArgumentExceptionIfUserIsOwner() {
        // given
        User user = User.builder().owner().build();

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("user cannot be an owner");
      }

      @Test
      public void shouldThrowNullPointerExceptionIfAdministeredByIsNull() {
        // given
        User user = User.builder().isOwner(false).administeredBy(null).build();

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK));

        // then
        assertThat(throwable).isInstanceOf(NullPointerException.class)
            .hasMessage("User#administeredBy must be non-null");
      }

      @Test
      public void shouldThrowIllegalArgumentExceptionIfUserIsEmailVerified() {
        // given
        User user = User.builder().administeredBy(UUID.randomUUID()).emailVerified(true).build();

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("user cannot have a verified email");
      }

      @ParameterizedTest
      @ValueSource(strings = {"", "  "})
      public void shouldThrowIllegalArgumentExceptionIfUserHasBlankEmail(String email) {
        // given
        User user = User.builder()
            .emailVerified(false)
            .administeredBy(UUID.randomUUID())
            .email(email)
            .build();

        // when
        Throwable throwable = catchThrowable(() ->
            passwordResetService
                .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("user must have a non-empty email");
      }
    }

    @Test
    public void shouldSaveTokenAndSendEmail() {
      // given
      String convertedCode = "some-code";
      mockExpirationDateProvider();
      mockTokenHashConverter(convertedCode);
      User user = User.builder()
          .id(UUID.randomUUID())
          .emailVerified(false)
          .administeredBy(UUID.randomUUID())
          .email("a@b.com")
          .build();

      // when
      passwordResetService
          .generateAndSaveResetTokenForInvitedUser(user, ANY_ACCOUNT, ANY_VERIFICATION_LINK);

      // then
      verify(accountUserCodeRepository).save(argThat(code ->
          CodeType.PASSWORD_RESET == code.getCodeType()
              && CodeStatus.ACTIVE == code.getStatus()
              && convertedCode.equals(code.getCode())
      ));
      verify(userInvitationEmailSender).send(
          eq(user.getEmail()),
          anyString(),
          argThat(context -> context.getAccount().isPresent())
      );
    }

    private void mockExpirationDateProvider() {
      when(tokensExpiryDatesProvider.getResetTokenExpiryDateFromNow())
          .thenReturn(LocalDateTime.now());
    }

    private void mockTokenHashConverter(String convertedCode) {
      when(tokenToHashConverter.convert(any())).thenReturn(convertedCode);
    }
  }
}
