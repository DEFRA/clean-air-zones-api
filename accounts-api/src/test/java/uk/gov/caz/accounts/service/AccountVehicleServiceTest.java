package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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

  private static final String ANY_ACCOUNT_ID = "c121e53f-3c7c-4873-b6c1-c0c13c7cef48";
  private static final String ANY_VRN = "CAS234";

  @Test
  public void throwsErrorIfTravelDirectionNull() {
    Mockito.when(accountRepository.findById(any()))
        .thenReturn(Optional.of(Account.builder().build()));
    Mockito.when(accountVehicleRepository.findByAccountIdAndVrn(any(), any()))
        .thenReturn(Optional.of(AccountVehicle.builder().build()));

    assertThrows(IllegalArgumentException.class, () -> {
      accountVehicleService.findVehiclesForAccountWithCursor(UUID.randomUUID().toString(),
          10, "test", null, any());
    });
  }

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

  @Test
  public void returnsAccountVehicleWhenSuccessfullyCreated() {
    // given
    mockExistingAccountWithoutAccountVehicle();

    // when
    AccountVehicle accountVehicle = accountVehicleService
        .createAccountVehicle(ANY_ACCOUNT_ID, ANY_VRN);

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
        () -> accountVehicleService.createAccountVehicle(ANY_ACCOUNT_ID, ANY_VRN));

    // then
    assertThat(throwable).isInstanceOf(AccountVehicleAlreadyExistsException.class)
        .hasMessage("AccountVehicle already exists");
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
        .accountId(UUID.fromString(ANY_ACCOUNT_ID))
        .cazVehicleType("sample-type")
        .vrn(ANY_VRN)
        .build();
  }
}
