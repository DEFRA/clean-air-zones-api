package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.AccountVehicleAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
public class AccountVehicleServiceTest {

  @InjectMocks
  AccountVehicleService accountVehicleService;

  @Mock
  AccountRepository accountRepository;

  @Mock
  AccountVehicleRepository accountVehicleRepository;

  @Mock
  ChargeCalculationService chargeCalculationService;

  private static final UUID ANY_ACCOUNT_ID = UUID
      .fromString("c121e53f-3c7c-4873-b6c1-c0c13c7cef48");
  private static final String ANY_VRN = "CAS234";
  private final int PAGE_SIZE = 10;
  private final int PAGE_NUMBER = 0;

  @Nested
  class GetAccountVehicle {

    @Test
    public void throwsErrorIfAccountIdNotFound() {
      UUID nonExistentAccountId = UUID.randomUUID();
      String testVrn = "TESTVRN";

      when(accountRepository.findById(nonExistentAccountId))
          .thenReturn(Optional.empty());

      assertThrows(AccountNotFoundException.class, () -> {
        accountVehicleService.getAccountVehicle(nonExistentAccountId.toString(), testVrn);
      });
    }
  }

  @Nested
  class CreateAccountVehicle {

    @Test
    public void returnsAccountVehicleWhenSuccessfullyCreated() {
      // given
      mockExistingAccountWithoutAccountVehicle();

      // when
      AccountVehicle accountVehicle = accountVehicleService
          .createAccountVehicle(ANY_ACCOUNT_ID.toString(), ANY_VRN);

      // then
      assertThat(accountVehicle.getVrn()).isEqualTo(ANY_VRN);
      verify(chargeCalculationService).populateCacheForSingleVehicle(any(UUID.class), eq(ANY_VRN));
    }

    @Test
    public void throwsErrorIfAccountVehicleAlreadyExists() {
      // given
      mockExistingAccountWithAccountVehicle();

      // when
      Throwable throwable = catchThrowable(
          () -> accountVehicleService.createAccountVehicle(ANY_ACCOUNT_ID.toString(), ANY_VRN));

      // then
      assertThat(throwable).isInstanceOf(AccountVehicleAlreadyExistsException.class)
          .hasMessage("AccountVehicle already exists");
    }
  }

  @Nested
  class FindVehiclesForAccount {

    @Test
    public void throwsErrorIfAccountDoesNotExist() {
      // given
      when(accountRepository.findById(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(() -> accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "", PAGE_NUMBER, PAGE_SIZE, true, true));

      // then
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account not found.");
    }

    @Test
    public void fetchesAllChargeableDeterminedVehiclesForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllDeterminedChargeableWithChargeabilityFor(any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "", PAGE_NUMBER, PAGE_SIZE, true, true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedChargeableWithChargeabilityFor(any(), any());
    }

    @Test
    public void fetchesChargeableDeterminedVehiclesForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllDeterminedChargeableByVrnContainingWithChargeabilityFor(any(), anyString(),
              any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "VRN", PAGE_NUMBER, PAGE_SIZE, true, true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedChargeableByVrnContainingWithChargeabilityFor(any(), anyString(),
              any());
    }

    @Test
    public void fetchesAllChargeableVehiclesForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllWithChargeabilityFor(any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "", PAGE_NUMBER, PAGE_SIZE, true, false);

      // then
      verify(accountVehicleRepository).findAllWithChargeabilityFor(any(), any());
    }

    @Test
    public void fetchesAllChargeableVehiclesForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllByVrnContainingWithChargeabilityFor(any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "VRN", PAGE_NUMBER, PAGE_SIZE, true, false);

      // then
      verify(accountVehicleRepository)
          .findAllByVrnContainingWithChargeabilityFor(any(), anyString(), any());

    }

    @Test
    public void fetchesAllDeterminedForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllDeterminedWithChargeabilityFor(any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "", PAGE_NUMBER, PAGE_SIZE, false, true);

      // then
      verify(accountVehicleRepository).findAllDeterminedWithChargeabilityFor(any(), any());
    }

    @Test
    public void fetchesAllDeterminedForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllDeterminedByVrnContainingWithChargeabilityFor(any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "VRN", PAGE_NUMBER, PAGE_SIZE, false, true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedByVrnContainingWithChargeabilityFor(any(), anyString(), any());

    }

    @Test
    public void fetchesAllVehiclesForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllByAccountId(any(), any())).thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "", PAGE_NUMBER, PAGE_SIZE, false, false);

      // then
      verify(accountVehicleRepository).findAllByAccountId(any(), any());
    }

    @Test
    public void fetchesAllVehiclesForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllByAccountIdAndVrnContaining(any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccount(ANY_ACCOUNT_ID, "VRN", PAGE_NUMBER, PAGE_SIZE, false, false);

      // then
      verify(accountVehicleRepository)
          .findAllByAccountIdAndVrnContaining(any(), anyString(), any());
    }
  }

  @Nested
  class FindVehiclesForAccountInCaz {

    private final UUID CAZ_ID = UUID.fromString("53e03a28-0627-11ea-9511-ffaaee87e375");

    @Test
    public void throwsErrorIfAccountDoesNotExist() {
      // given
      when(accountRepository.findById(any())).thenReturn(Optional.empty());

      // when
      Throwable throwable = catchThrowable(() -> accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, true,
              true));

      // then
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account not found.");
    }

    @Test
    public void fetchesAllChargeableDeterminedVehiclesInCazForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllDeterminedChargeableForAccountInCaz(any(), any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, true,
              true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedChargeableForAccountInCaz(any(), any(), any());
    }

    @Test
    public void fetchesChargeableDeterminedVehiclesInCazForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllDeterminedChargeableByVrnForAccountInCaz(any(), any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "VRN", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, true,
              true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedChargeableByVrnForAccountInCaz(any(), any(), anyString(), any());
    }

    @Test
    public void fetchesChargeableVehiclesInCazForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository.findAllChargeableForAccountInCaz(any(), any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, true,
              false);

      // then
      verify(accountVehicleRepository).findAllChargeableForAccountInCaz(any(), any(), any());
    }

    @Test
    public void fetchesChargeableVehiclesInCazForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllChargeableByVrnForAccountInCaz(any(), any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "VRN", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, true,
              false);

      // then
      verify(accountVehicleRepository)
          .findAllChargeableByVrnForAccountInCaz(any(), any(), anyString(), any());
    }

    @Test
    public void fetchesDeterminedVehiclesInCazForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllDeterminedForAccountInCaz(any(), any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, false,
              true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedForAccountInCaz(any(), any(), any());

    }

    @Test
    public void fetchesDeterminedVehiclesInCazForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllDeterminedByVrnForAccountInCaz(any(), any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "VRN", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, false,
              true);

      // then
      verify(accountVehicleRepository)
          .findAllDeterminedByVrnForAccountInCaz(any(), any(), anyString(), any());
    }

    @Test
    public void fetchesAllVehiclesInCazForEmptyQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllByAccountIdAndCaz(any(), any(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, false,
              false);

      // then
      verify(accountVehicleRepository)
          .findAllByAccountIdAndCaz(any(), any(), any());

    }

    @Test
    public void fetchesAllVehiclesInCazForExistingQuery() {
      // given
      mockExistingAccount();
      when(accountVehicleRepository
          .findAllByAccountIdAndVrnContainingInCaz(any(), any(), anyString(), any()))
          .thenReturn(Page.empty());

      // when
      accountVehicleService
          .findVehiclesForAccountInCaz(ANY_ACCOUNT_ID, "VRN", CAZ_ID, PAGE_NUMBER, PAGE_SIZE, false,
              false);

      // then
      verify(accountVehicleRepository)
          .findAllByAccountIdAndVrnContainingInCaz(any(), any(), anyString(), any());
    }
  }

  private void mockExistingAccount() {
    when(accountRepository.findById(any()))
        .thenReturn(Optional.of(Account.builder().build()));
  }

  private void mockExistingAccountWithAccountVehicle() {
    when(accountRepository.findById(any()))
        .thenReturn(Optional.of(Account.builder().build()));
    when(accountVehicleRepository.findByAccountIdAndVrn(any(), any()))
        .thenReturn(Optional.of(sampleAccountVehicle()));
  }

  private void mockExistingAccountWithoutAccountVehicle() {
    when(accountRepository.findById(any()))
        .thenReturn(Optional.of(Account.builder().build()));
    when(accountVehicleRepository.findByAccountIdAndVrn(any(), any()))
        .thenReturn(Optional.empty());
    when(accountVehicleRepository.save(any())).thenReturn(sampleAccountVehicle());
  }

  private AccountVehicle sampleAccountVehicle() {
    return AccountVehicle.builder()
        .accountVehicleId(UUID.randomUUID())
        .accountId(ANY_ACCOUNT_ID)
        .cazVehicleType("sample-type")
        .vrn(ANY_VRN)
        .build();
  }
}
