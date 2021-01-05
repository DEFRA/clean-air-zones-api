package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.ProhibitedTermType;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.service.exception.AbusiveNameException;
import uk.gov.caz.accounts.service.exception.AccountAlreadyExistsException;
import uk.gov.caz.accounts.service.validation.DuplicateAccountValidator;

@ExtendWith(MockitoExtension.class)
public class AccountCreatorServiceTest {

  private static final String EMAIL = "dev@jaqu.gov";
  private static final String ACCOUNT_NAME = "account name";
  private static final String PASSWORD = "passw00rd";

  @Mock
  private UserService userService;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AbusiveLanguageValidator abusiveLanguageValidator;

  @Mock
  private DuplicateAccountValidator duplicateAccountValidator;

  @InjectMocks
  private AccountCreatorService accountCreatorService;

  @Test
  public void shouldThrowNullPointerExceptionWhenUserIsNull() {
    // given
    String accountName = null;

    // when
    Throwable throwable = catchThrowable(
        () -> accountCreatorService.createAccount(accountName));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("'accountName' cannot be null");
  }

  @Test
  public void shouldReturnTrueIfEverythingGoesWell() {
    //given
    UUID accountId = mockAccountCreation(ACCOUNT_NAME);

    //when
    Account account = accountCreatorService.createAccount(ACCOUNT_NAME);

    //then
    assertThat(account.getId()).isEqualTo(accountId);
    verify(userService, never()).createAdminUser(EMAIL, PASSWORD, accountId);
  }

  @Test
  public void shouldNotCreateAnAccountWhenValidationForDuplicateAccountFailed() {
    doThrow(new AccountAlreadyExistsException(RandomStringUtils.randomAlphabetic(5)))
        .when(duplicateAccountValidator).validate(eq(ACCOUNT_NAME));

  Throwable throwable = catchThrowable(() -> accountCreatorService.createAccount(ACCOUNT_NAME));

    assertThat(throwable).isInstanceOf(AccountAlreadyExistsException.class);
    verifyNoInteractions(accountRepository);
  }

  @Test
  public void shouldNotCreateAnAccountWhenValidationForAbusiveLanguageFailed() {
    doThrow(new AbusiveNameException(ProhibitedTermType.ABUSE))
        .when(abusiveLanguageValidator).validate(eq(ACCOUNT_NAME));

    Throwable throwable = catchThrowable(() -> accountCreatorService.createAccount(ACCOUNT_NAME));

    assertThat(throwable).isInstanceOf(AbusiveNameException.class);
    verifyNoInteractions(accountRepository);
  }

  @Test
  public void shouldNotLowerCaseAccountNameAllCaps() {
    // given
    String allCapsAccountName = "THE BEST COMPANY HAS ALL CAPS IN NAME";
    mockAccountCreation(allCapsAccountName);
    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    accountCreatorService.createAccount(allCapsAccountName);

    // then
    verify(accountRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo(allCapsAccountName);
  }

  @Test
  public void shouldNotLowerCaseAccountNameCapitalised() {
    // given
    String allCapsAccountName = "The Good Enough Company's Name Is Just Capitilised";
    mockAccountCreation(allCapsAccountName);
    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

    // when
    accountCreatorService.createAccount(allCapsAccountName);

    // then
    verify(accountRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo(allCapsAccountName);
  }


  private UUID mockAccountCreation(String accountName) {
    Account account = Account.builder().name(accountName).build();
    Account createdAccount = account.toBuilder().id(UUID.randomUUID()).build();
    when(accountRepository.save(any())).thenReturn(createdAccount);

    return createdAccount.getId();
  }

}
