package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateDoesNotBelongsToAccountException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateNotFoundException;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandateServiceTest {

  @Mock
  DirectDebitMandateRepository directDebitMandateRepository;

  @Mock
  AccountRepository accountRepository;

  @Mock
  UserRepository userRepository;

  @InjectMocks
  DirectDebitMandateService directDebitMandateService;

  private static final UUID ANY_ID = UUID.fromString("f08400df-343a-463d-8a7d-a7b3123de3cb");
  private static final UUID VALID_ACCOUNT_ID = UUID
      .fromString("9a622b4b-1750-432c-be56-d2455256f7ae");
  private static final UUID INVALID_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_CLEAN_AIR_ZONE_ID = UUID
      .fromString("eec6f890-98f4-42f7-980c-6bb2efaf621a");
  private static final UUID ANY_ACCOUNT_USER_ID = UUID
      .fromString("f64a06aa-347b-4852-966a-1441b04679f0");
  private static final UUID VALID_DIRECT_DEBIT_MANDATE_ID = UUID.randomUUID();
  private static final String ANY_PAYMENT_PROVIDER_MANDATE_ID = "45368d746ace3be30229";

  @Nested
  class Create {

    @Test
    public void shouldReturnPersistedDirectDebitMandate() {
      // given
      mockPresentAccount();
      mockPresentAccountUser();
      mockDirectDebitMandateSave();

      // when
      DirectDebitMandate directDebitMandate =
          directDebitMandateService.create(VALID_ACCOUNT_ID, directDebitMandateRequest());

      // then
      assertThat(directDebitMandate.getId()).isNotNull();
      verify(directDebitMandateRepository).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenAnAccountWithProvidedAccountIdDoesNotExist() {
      // given
      mockMissingAccount();

      // when
      Throwable throwable = catchThrowable(
          () -> directDebitMandateService.create(INVALID_ACCOUNT_ID, directDebitMandateRequest()));

      // then
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    @Test
    public void shouldThrowExceptionWhenAnAccountWithProvidedAccountUserIdDoesNotExist() {
      // given
      mockPresentAccount();
      mockMissingAccountUser();

      // when
      Throwable throwable = catchThrowable(
          () -> directDebitMandateService.create(VALID_ACCOUNT_ID, directDebitMandateRequest()));

      // then
      assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
          .hasMessage("AccountUser was not found.");
    }

    private DirectDebitMandateRequest directDebitMandateRequest() {
      return DirectDebitMandateRequest.builder()
          .mandateId(ANY_PAYMENT_PROVIDER_MANDATE_ID)
          .accountUserId(ANY_ACCOUNT_USER_ID)
          .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
          .build();
    }

    private void mockDirectDebitMandateSave() {
      when(directDebitMandateRepository.save(any()))
          .thenReturn(persistedDirectDebitMandateMock());
    }

    private void mockMissingAccountUser() {
      when(userRepository.findByIdAndAccountId(any(), any())).thenReturn(Optional.empty());
    }

    private void mockPresentAccountUser() {
      when(userRepository.findByIdAndAccountId(any(), any()))
          .thenReturn(Optional.of(UserEntity.builder().build()));
    }

    private DirectDebitMandate persistedDirectDebitMandateMock() {
      return DirectDebitMandate.builder()
          .id(ANY_ID)
          .accountId(VALID_ACCOUNT_ID)
          .accountUserId(ANY_ACCOUNT_USER_ID)
          .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
          .paymentProviderMandateId(ANY_PAYMENT_PROVIDER_MANDATE_ID)
          .build();
    }
  }

  @Nested
  class FindAllByAccountId {

    @Test
    public void shouldReturnPersistedDirectDebitMandates() {
      // given
      mockDirectDebitMandateSuccessFind();

      // when
      List<DirectDebitMandate> directDebitMandates =
          directDebitMandateService.findAllByAccountId(VALID_ACCOUNT_ID);

      // then
      assertThat(directDebitMandates).isNotEmpty();
      verify(directDebitMandateRepository).findAllByAccountId(any());
    }

    @Test
    public void shouldReturnEmptyListIfNotExists() {
      // given
      mockDirectDebitMandateNotFound();

      // when
      List<DirectDebitMandate> directDebitMandates =
          directDebitMandateService.findAllByAccountId(INVALID_ACCOUNT_ID);

      // then
      assertThat(directDebitMandates).isEmpty();
      verify(directDebitMandateRepository).findAllByAccountId(any());
    }

    private void mockDirectDebitMandateNotFound() {
      when(directDebitMandateRepository.findAllByAccountId(any()))
          .thenReturn(Collections.emptyList());
    }


    private void mockDirectDebitMandateSuccessFind() {
      when(directDebitMandateRepository.findAllByAccountId(any()))
          .thenReturn(persistedDirectDebitMandatesMock());
    }

    private List<DirectDebitMandate> persistedDirectDebitMandatesMock() {
      return Arrays.asList(DirectDebitMandate.builder()
          .id(ANY_ID)
          .accountId(VALID_ACCOUNT_ID)
          .accountUserId(ANY_ACCOUNT_USER_ID)
          .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
          .paymentProviderMandateId(ANY_PAYMENT_PROVIDER_MANDATE_ID)
          .build());
    }
  }

  @Nested
  class Delete {

    @Test
    public void shouldThrowExceptionWhenAnAccountWithProvedAccountIdDoesNotExist() {
      // given
      mockMissingAccount();

      // when
      Throwable throwable = catchThrowable(
          () -> directDebitMandateService
              .delete(INVALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID));

      // then
      assertThat(throwable).isInstanceOf(AccountNotFoundException.class)
          .hasMessage("Account was not found.");
    }

    @Test
    public void shouldThrowExceptionWhenAnAccountWithProvedMandateDoesNotExist() {
      // given
      mockPresentAccount();
      mockMissingDirectDebitMandate();

      // when
      Throwable throwable = catchThrowable(
          () -> directDebitMandateService.delete(VALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID));

      // then
      assertThat(throwable).isInstanceOf(DirectDebitMandateNotFoundException.class)
          .hasMessage("Direct Debit Mandate not found.");
    }

    @Test
    public void shouldThrowExceptionWhenMandateDoesNotBelongsToAccount() {
      // given
      mockPresentAccount();
      mockPresentDirectDebitMandate();

      // when
      Throwable throwable = catchThrowable(
          () -> directDebitMandateService
              .delete(INVALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID));

      // then
      assertThat(throwable).isInstanceOf(DirectDebitMandateDoesNotBelongsToAccountException.class)
          .hasMessage("DirectDebitMandate does not belongs to provided Account");
    }

    @Test
    public void shouldDeleteDirectDebitMandate() {
      // given
      mockPresentAccount();
      mockPresentDirectDebitMandate();
      mockDirectDebitMandateDelete();

      // when
      directDebitMandateService.delete(VALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID);

      // then
      verify(directDebitMandateRepository).delete(any());
    }


    private void mockMissingDirectDebitMandate() {
      when(directDebitMandateRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    }

    private void mockPresentDirectDebitMandate() {
      when(directDebitMandateRepository.findById(any(UUID.class)))
          .thenReturn(Optional.of(persistedDirectDebitMandateMock()));
    }

    private void mockDirectDebitMandateDelete() {
      doNothing().when(directDebitMandateRepository).delete(any());
    }

    private DirectDebitMandate persistedDirectDebitMandateMock() {
      return DirectDebitMandate.builder()
          .id(ANY_ID)
          .accountId(VALID_ACCOUNT_ID)
          .accountUserId(ANY_ACCOUNT_USER_ID)
          .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
          .paymentProviderMandateId(ANY_PAYMENT_PROVIDER_MANDATE_ID)
          .build();
    }
  }

  private void mockPresentAccount() {
    Account mockedAccount = Account.builder().build();
    when(accountRepository.findById(any())).thenReturn(Optional.of(mockedAccount));
  }

  private void mockMissingAccount() {
    when(accountRepository.findById(any())).thenReturn(Optional.empty());
  }
}