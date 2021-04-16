package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.DirectDebitMandateUpdateError;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateUpdateException;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandatesBulkUpdaterTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private DirectDebitMandateRepository directDebitMandateRepository;

  @Mock
  private DirectDebitMandateUpdateErrorsCollector directDebitMandateUpdateErrorsCollector;

  @InjectMocks
  private DirectDebitMandatesBulkUpdater directDebitMandatesBulkUpdater;

  private static final UUID ANY_ACCOUNT_ID = UUID
      .fromString("c3dc7458-1089-4984-95a8-1c40e54c572b");
  private static final UUID ANY_ACCOUNT_USER_ID = UUID
      .fromString("f64a06aa-347b-4852-966a-1441b04679f0");
  private static final String ANY_MANDATE_ID = "dsvbasdhodsaifha98";

  @Test
  public void shouldThrowAccountNotFoundExceptionWhenAccountIsMissing() {
    // given
    mockNotFoundAccount();
    List<SingleDirectDebitMandateUpdate> updatesList = Collections.emptyList();

    // when
    Throwable throwable = catchThrowable(() ->
        directDebitMandatesBulkUpdater.updateStatuses(ANY_ACCOUNT_ID, updatesList));

    // then
    assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
        .hasMessage("Account was not found.");
  }

  @Test
  public void shouldThrowDirectDebitMandateUpdateExceptionWhenValidationFails() {
    // given
    mockFoundAccount();
    List<SingleDirectDebitMandateUpdate> updatesList = Collections.emptyList();
    mockFailedValidation();

    // when
    Throwable throwable = catchThrowable(() ->
        directDebitMandatesBulkUpdater.updateStatuses(ANY_ACCOUNT_ID, updatesList));

    // then
    assertThat(throwable).isInstanceOf(DirectDebitMandateUpdateException.class);
  }

  @Test
  public void shouldUpdateRecordsWhenNoValidationErrorsWereFound() {
    // given
    mockFoundAccount();
    List<SingleDirectDebitMandateUpdate> updatesList = updatesListMock();
    mockSuccessfulValidation();
    mockAssociatedDirectDebitMandateWithAccount();

    // when
    directDebitMandatesBulkUpdater.updateStatuses(ANY_ACCOUNT_ID, updatesList);

    // then
    verify(directDebitMandateRepository).saveAll(any());
  }

  @Test
  public void shouldThrowIllegalStateExceptionIfMandateIsNotFound() {
    // given
    mockFoundAccount();
    mockSuccessfulValidation();
    mockAbsenceOfAssociatedDirectDebitMandateWithAccount();
    List<SingleDirectDebitMandateUpdate> updatesList = updatesListMock();

    // when
    Throwable throwable = catchThrowable(() ->
        directDebitMandatesBulkUpdater.updateStatuses(ANY_ACCOUNT_ID, updatesList));

    // when
    assertThat(throwable).isInstanceOf(IllegalStateException.class)
        .hasMessage("Mandate does not exist");
  }

  private void mockAbsenceOfAssociatedDirectDebitMandateWithAccount() {
    when(directDebitMandateRepository.findByPaymentProviderMandateId(any()))
        .thenReturn(Optional.empty());
  }

  private void mockAssociatedDirectDebitMandateWithAccount() {
    DirectDebitMandate mandate = DirectDebitMandate.builder()
        .status(DirectDebitMandateStatus.SUBMITTED)
        .cleanAirZoneId(UUID.randomUUID())
        .accountUserId(ANY_ACCOUNT_USER_ID)
        .paymentProviderMandateId(ANY_MANDATE_ID)
        .build();

    when(directDebitMandateRepository.findByPaymentProviderMandateId(any()))
        .thenReturn(Optional.of(mandate));
  }

  private List<SingleDirectDebitMandateUpdate> updatesListMock() {
    SingleDirectDebitMandateUpdate singleUpdate = SingleDirectDebitMandateUpdate
        .builder()
        .mandateId(ANY_MANDATE_ID)
        .status(DirectDebitMandateStatus.ACTIVE.toString())
        .build();
    return Arrays.asList(singleUpdate);
  }

  private void mockNotFoundAccount() {
    when(accountRepository.findById(any())).thenReturn(Optional.empty());
  }

  private void mockFoundAccount() {
    when(accountRepository.findById(any()))
        .thenReturn(Optional.of(Account.builder().build()));
  }

  private void mockFailedValidation() {
    List<DirectDebitMandateUpdateError> errorsList = Arrays.asList(
        DirectDebitMandateUpdateError.invalidMandateStatus(ANY_MANDATE_ID)
    );

    when(directDebitMandateUpdateErrorsCollector.collectErrors(any(), any()))
        .thenReturn(errorsList);
  }

  private void mockSuccessfulValidation() {
    when(directDebitMandateUpdateErrorsCollector.collectErrors(any(), any()))
        .thenReturn(Collections.emptyList());
  }
}