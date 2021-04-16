package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandateUpdateErrorsCollectorTest {

  @Mock
  DirectDebitMandateRepository directDebitMandateRepository;

  @InjectMocks
  DirectDebitMandateUpdateErrorsCollector directDebitMandateUpdateErrorsCollector;

  private static final UUID ANY_ACCOUNT_ID = UUID
      .fromString("1a9838f6-985e-4b19-9874-954caef1c4fe");
  private static final UUID ANY_ACCOUNT_USER_ID = UUID
      .fromString("f64a06aa-347b-4852-966a-1441b04679f0");
  private static final String ANY_MANDATE_ID = "jhjcvaiqlediuhh23d89hd3";
  private static final String ANY_STATUS = DirectDebitMandateStatus.ACTIVE.toString();

  @Test
  public void shouldReturnEmptyListWhenProvidedDataAreValid() {
    // given
    mockValidAccountAndMandateAssociation();
    List<SingleDirectDebitMandateUpdate> updatesList =
        buildUpdatesListWith(ANY_MANDATE_ID, ANY_STATUS);

    // when
    List<DirectDebitMandateUpdateError> errors = directDebitMandateUpdateErrorsCollector
        .collectErrors(ANY_ACCOUNT_ID, updatesList);

    // then
    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldReturnInvalidStatusErrorWhenUpdateHasWrongStatus() {
    // given
    mockValidAccountAndMandateAssociation();
    List<SingleDirectDebitMandateUpdate> updatesList =
        buildUpdatesListWith(ANY_MANDATE_ID, "INVALID-STATUS");

    // when
    List<DirectDebitMandateUpdateError> errors = directDebitMandateUpdateErrorsCollector
        .collectErrors(ANY_ACCOUNT_ID, updatesList);

    // then
    assertThat(errors.size()).isEqualTo(1);
    assertThat(errors.get(0).getMessage()).isEqualTo("Invalid direct debit mandate status");
  }

  @Test
  public void shouldReturnMissingMandateErrorWhenMandateWithProvidedIdDoesNotExist() {
    // given
    List<SingleDirectDebitMandateUpdate> updatesList =
        buildUpdatesListWith(ANY_MANDATE_ID, ANY_STATUS);

    // when
    List<DirectDebitMandateUpdateError> errors = directDebitMandateUpdateErrorsCollector
        .collectErrors(ANY_ACCOUNT_ID, updatesList);

    // then
    assertThat(errors.size()).isEqualTo(1);
    assertThat(errors.get(0).getMessage()).isEqualTo("Direct debit mandate does not exist");
  }

  @Test
  public void shouldReturnAccountAndMandateMismatchErrorWhenProvidedMandateDoesNotBelongToAccount() {
    // given
    mockInvalidAccountAndMandateAssociation();
    List<SingleDirectDebitMandateUpdate> updatesList =
        buildUpdatesListWith(ANY_MANDATE_ID, ANY_STATUS);

    // when
    List<DirectDebitMandateUpdateError> errors = directDebitMandateUpdateErrorsCollector
        .collectErrors(ANY_ACCOUNT_ID, updatesList);

    // then
    assertThat(errors.size()).isEqualTo(1);
    assertThat(errors.get(0).getMessage())
        .isEqualTo("Direct debit mandate does not belong to this account.");
  }

  @Test
  public void shouldReturnAllErrorsWhenThereAreMoreThanOne() {
    // given
    mockInvalidAccountAndMandateAssociation();
    List<SingleDirectDebitMandateUpdate> updatesList =
        buildUpdatesListWith(ANY_MANDATE_ID, "INVALID-STATUS");

    // when
    List<DirectDebitMandateUpdateError> errors = directDebitMandateUpdateErrorsCollector
        .collectErrors(ANY_ACCOUNT_ID, updatesList);

    // then
    assertThat(errors.size()).isEqualTo(2);

  }

  private List<SingleDirectDebitMandateUpdate> buildUpdatesListWith(String mandateId,
      String status) {
    return Arrays.asList(SingleDirectDebitMandateUpdate.builder()
        .mandateId(mandateId)
        .status(status)
        .build());
  }

  private void mockValidAccountAndMandateAssociation() {
    when(directDebitMandateRepository.findByPaymentProviderMandateId(any()))
        .thenReturn(Optional.of(createdDirectDebitMandateMockFor(ANY_ACCOUNT_ID)));
  }

  private void mockInvalidAccountAndMandateAssociation() {
    when(directDebitMandateRepository.findByPaymentProviderMandateId(any()))
        .thenReturn(Optional.of(createdDirectDebitMandateMockFor(UUID.randomUUID())));
  }

  private DirectDebitMandate createdDirectDebitMandateMockFor(UUID accountId) {
    DirectDebitMandate mandate = DirectDebitMandate.builder()
        .id(UUID.randomUUID())
        .accountId(accountId)
        .accountUserId(ANY_ACCOUNT_USER_ID)
        .cleanAirZoneId(UUID.randomUUID())
        .paymentProviderMandateId(ANY_MANDATE_ID)
        .status(DirectDebitMandateStatus.SUBMITTED)
        .build();

    return mandate;
  }
}