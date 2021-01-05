package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.ProhibitedTermType;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AbusiveNameException;
import uk.gov.caz.accounts.service.exception.AccountAlreadyExistsException;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.validation.DuplicateAccountValidator;

@ExtendWith(MockitoExtension.class)
class AccountUpdateServiceTest {

  private static final String ACCOUNT_NAME = "account name";
  private static final UUID ACCOUNT_ID = UUID.randomUUID();

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AbusiveLanguageValidator abusiveLanguageValidator;

  @Mock
  private DuplicateAccountValidator duplicateAccountValidator;

  @InjectMocks
  private AccountUpdateService accountUpdateService;

  @Test
  public void shouldUpdateAccountNameWhenAllParametersAreValid() {
    // given
    accountIsPresent();

    // when
    accountUpdateService.updateAccountName(ACCOUNT_ID, ACCOUNT_NAME);

    // then
    verify(accountRepository).updateName(ACCOUNT_ID, ACCOUNT_NAME);
  }


  @Test
  public void shouldThrowNullPointerExceptionWhenAccountNameIsNull() {
    // given
    accountIsPresent();
    String accountName = null;

    // when
    Throwable throwable = catchThrowable(
        () -> accountUpdateService.updateAccountName(ACCOUNT_ID, accountName));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("'accountName' cannot be null");
  }

  @Test
  public void shouldNotUpdateAccountNameWhenValidationForDuplicateAccountFailed() {
    // given
    accountIsPresent();
    doThrow(new AccountAlreadyExistsException(RandomStringUtils.randomAlphabetic(5)))
        .when(duplicateAccountValidator).validate(eq(ACCOUNT_NAME));

    // when
    Throwable throwable = catchThrowable(
        () -> accountUpdateService.updateAccountName(ACCOUNT_ID, ACCOUNT_NAME));

    // then
    assertThat(throwable).isInstanceOf(AccountAlreadyExistsException.class);
  }

  @Test
  public void shouldNotUpdateAccountNameWhenValidationForAbusiveLanguageFailed() {
    // given
    accountIsPresent();
    doThrow(new AbusiveNameException(ProhibitedTermType.ABUSE))
        .when(abusiveLanguageValidator).validate(eq(ACCOUNT_NAME));

    // when
    Throwable throwable = catchThrowable(
        () -> accountUpdateService.updateAccountName(ACCOUNT_ID, ACCOUNT_NAME));

    // then
    assertThat(throwable).isInstanceOf(AbusiveNameException.class);
  }

  @Test
  public void shouldThrowAccountNotFoundExceptionWhenAccountIsNotPresent() {
    // given
    accountIsNotPresent();
    String accountName = "does not matter";

    // when
    Throwable throwable = catchThrowable(
        () -> accountUpdateService.updateAccountName(ACCOUNT_ID, accountName));

    // then
    assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account was not found.");
  }

  private void accountIsPresent() {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(Account.builder().build()));
  }

  private void accountIsNotPresent() {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
  }
}