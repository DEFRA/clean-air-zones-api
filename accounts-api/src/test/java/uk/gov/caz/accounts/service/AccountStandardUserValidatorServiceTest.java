package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.controller.exception.EmailNotUniqueException;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;

@ExtendWith(MockitoExtension.class)
class AccountStandardUserValidatorServiceTest {

  private static final String ANY_EMAIL = "dev@jaqu.gov";
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Mock
  private UserService userService;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @InjectMocks
  private AccountStandardUserValidatorService accountStandardUserValidatorService;

  @Nested
  class InputValidation {

    @Test
    public void shouldThrowNullPointerExceptionWhenEmailIsNull() {
      Throwable throwable = catchThrowable(() -> accountStandardUserValidatorService
          .validateUserEmail(null));
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("email cannot be null");
    }
  }

  @Nested
  class UserWithEmailAlreadyExists {

    @Nested
    class AndIsVerified {

      @Test
      public void shouldThrowEmailNotUniqueException() {
        // given
        mockExitingUserWithEmail(true);

        // when
        Throwable throwable = catchThrowable(() -> accountStandardUserValidatorService
            .validateUserEmail(ANY_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(EmailNotUniqueException.class)
            .hasMessage("Provided email is not unique");

      }
    }

    @Nested
    class AndIsNotVerified {

      @Test
      public void shouldThrowEmailNotUniqueExceptionWhenUserHasActiveVerificationCode() {
        // given
        mockExitingUserWithEmail(false);
        mockActiveAccountUserCodes();

        // when
        Throwable throwable = catchThrowable(() -> accountStandardUserValidatorService
            .validateUserEmail(ANY_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(EmailNotUniqueException.class)
            .hasMessage("Provided email is not unique");

      }
    }
  }

  private void mockExitingUserWithEmail(boolean emailVerified) {
    UserEntity user = UserEntity.builder()
        .id(ANY_ACCOUNT_USER_ID)
        .emailVerified(emailVerified)
        .build();
    when(userService.getUserEntityByEmail(ANY_EMAIL)).thenReturn(Optional.of(user));
  }

  private void mockActiveAccountUserCodes() {
    AccountUserCode accountUserCode = AccountUserCode.builder()
        .accountUserId(ANY_ACCOUNT_USER_ID)
        .code("Code")
        .expiration(LocalDateTime.now().plusDays(1))
        .codeType(CodeType.USER_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .build();

    when(accountUserCodeRepository
        .findByAccountUserIdAndStatusAndCodeTypeIn(ANY_ACCOUNT_USER_ID, CodeStatus.ACTIVE,
            newArrayList(CodeType.USER_VERIFICATION, CodeType.PASSWORD_RESET)))
        .thenReturn(Collections.singletonList(accountUserCode));
  }
}