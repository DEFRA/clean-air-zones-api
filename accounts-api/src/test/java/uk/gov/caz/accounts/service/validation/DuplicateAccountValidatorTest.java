package uk.gov.caz.accounts.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.exception.AccountAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
public class DuplicateAccountValidatorTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AccountUserCodeRepository userCodeRepository;

  @InjectMocks
  private DuplicateAccountValidator validator;

  String accountName;
  UUID accountId;
  Account account;

  @BeforeEach
  public void setup() {
    accountName = RandomStringUtils.randomAlphabetic(10).toLowerCase();
    accountId = UUID.randomUUID();
    account = Account.builder().name(accountName).id(accountId).build();
  }

  @Nested
  class ShouldNotThrowAnyErrorWhen {

    @Test
    public void accountDoesNotExists() {
      // given
      when(accountRepository.findAllByNameIgnoreCase(eq(accountName)))
          .thenReturn(Collections.emptyList());

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isNull();
    }

    @Test
    public void thereAreNoUsers() {
      // given
      mockWithUserCodeList(Collections.emptyList());

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isNull();
    }

    @Test
    public void thereAreUnverifiedUsersWhoseLinkExpired() {
      // given
      List<AccountUserCode> oneCodeFromThePast = Lists.newArrayList(
          userCodeOf(CodeStatus.ACTIVE, LocalDateTime.now().minusDays(1))
      );
      mockWithUserCodeList(oneCodeFromThePast);

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isNull();
    }

    @Test
    public void thereAreUsersWhoseLinkIsNotExpiredButStatusIsDiscarded() {
      // given
      List<AccountUserCode> oneCodeFromThePast = Lists.newArrayList(
          userCodeOf(CodeStatus.DISCARDED, LocalDateTime.now().plusDays(1))
      );
      mockWithUserCodeList(oneCodeFromThePast);

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isNull();
    }
  }


  @Nested
  class ShouldThrowValidationErrorWhen {

    @Test
    public void thereAreVerifiedUsers() {
      // given
      List<AccountUserCode> oneUsedCode = Lists.newArrayList(
          userCodeOf(CodeStatus.USED, LocalDateTime.now())
      );
      mockWithUserCodeList(oneUsedCode);

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isInstanceOf(AccountAlreadyExistsException.class);
      assertThat(throwable).hasMessageContaining("verified");
    }

    @Test
    public void thereAreUnverifiedUsersWithExpirationDateInTheFuture() {
      // given
      List<AccountUserCode> oneCodeFromTheFuture = Lists.newArrayList(
          userCodeOf(CodeStatus.ACTIVE, LocalDateTime.now().plusDays(1))
      );
      mockWithUserCodeList(oneCodeFromTheFuture);

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(accountName));

      // then
      assertThat(throwable).isInstanceOf(AccountAlreadyExistsException.class);
      assertThat(throwable).hasMessageContaining("pending");
    }
  }


  private void mockWithUserCodeList(List<AccountUserCode> codes) {
    when(accountRepository.findAllByNameIgnoreCase(eq(accountName)))
        .thenReturn(Collections.singletonList(account));
    when(userCodeRepository.findAllByAccountNameForExistingUsers(eq(accountName))).thenReturn(codes);
  }

  private AccountUserCode userCodeOf(CodeStatus codeStatus,
      LocalDateTime expiration) {
    UUID userId = UUID.randomUUID();
    AccountUserCode userCode = AccountUserCode.builder()
        .status(codeStatus)
        .accountUserId(userId)
        .code(RandomStringUtils.randomAlphabetic(5))
        .expiration(expiration)
        .codeType(CodeType.PASSWORD_RESET)
        .build();
    return userCode;
  }
}
