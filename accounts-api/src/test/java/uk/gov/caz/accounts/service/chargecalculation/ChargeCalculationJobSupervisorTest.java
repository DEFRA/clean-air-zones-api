package uk.gov.caz.accounts.service.chargecalculation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.service.ChargeCalculationService;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.emailnotifications.ChargeCalculationCompleteEmailSender;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;

@ExtendWith(MockitoExtension.class)
class ChargeCalculationJobSupervisorTest {

  private static final UUID ANY_ACCOUNT_ID = UUID.fromString("de798253-4b7a-4a6f-91a4-c7c3f0e1c4e1");
  private static final UUID ANY_CORRELATION_ID = UUID.fromString("d69e2371-3020-4b2b-8d4e-9b4b2f0f7cd3");
  private static final int ANY_INVOCATION_NUMBER = 4;
  private static final int ANY_JOB_ID = 97244;
  private static final String ANY_EMAIL = "a@b.com";
  private static final boolean ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG = false;

  private static final int MAX_INVOCATION_COUNT = 10;
  private static final int MAX_VEHICLES_TO_PROCESS = 20;

  @Mock
  private ChargeCalculationService chargeCalculationService;
  @Mock
  private AsyncChargeCalculationStarter asyncChargeCalculationStarter;
  @Mock
  private RegisterJobSupervisor registerJobSupervisor;
  @Mock
  private ChargeCalculationCompleteEmailSender chargeCalculationCompleteEmailSender;
  @Mock
  private UserService userService;


  private ChargeCalculationJobSupervisor chargeCalculationJobSupervisor;

  @BeforeEach
  public void setUp() {
    chargeCalculationJobSupervisor = new ChargeCalculationJobSupervisor(chargeCalculationService,
        asyncChargeCalculationStarter, registerJobSupervisor, chargeCalculationCompleteEmailSender,
        userService, MAX_INVOCATION_COUNT, MAX_VEHICLES_TO_PROCESS);
  }


  @Nested
  class WhenCachePopulationHasNotBeenFinished {

    @Nested
    class AndInvocationNumberHasNotBeenReached {

      @Test
      public void shouldInvokeAnotherLambdaToProcessNextBatch() {
        // given
        mockNotFinishedCaching();

        // when
        chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, ANY_JOB_ID,
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID, ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG);

        // then
        verify(asyncChargeCalculationStarter).fireAndForget(ANY_ACCOUNT_ID, ANY_JOB_ID,
            ANY_CORRELATION_ID, ANY_INVOCATION_NUMBER + 1,
            ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG);

        verify(chargeCalculationCompleteEmailSender, never()).send(any());
        verify(registerJobSupervisor, never()).updateStatus(anyInt(), any());
      }
    }

    @Nested
    class AndInvocationNumberHasBeenReached {

      @ParameterizedTest
      @ValueSource(ints = {MAX_VEHICLES_TO_PROCESS, MAX_VEHICLES_TO_PROCESS + 1,
          MAX_VEHICLES_TO_PROCESS + 269})
      public void shouldNotInvokeAnotherLambdaToProcessNextBatch(int invocationNumber) {
        // given
        mockNotFinishedCaching();

        // when
        chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, ANY_JOB_ID,
            invocationNumber, ANY_CORRELATION_ID, ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG);

        // then
        verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), any(), any(), anyInt(),
            anyBoolean());
        verify(registerJobSupervisor).updateStatus(ANY_JOB_ID,
            RegisterJobStatus.FINISHED_FAILURE_MAX_INVOCATION_COUNT_REACHED);

        verify(chargeCalculationCompleteEmailSender, never()).send(any());
      }
    }

    private void mockNotFinishedCaching() {
      given(chargeCalculationService.populateCache(ANY_ACCOUNT_ID, MAX_VEHICLES_TO_PROCESS))
          .willReturn(CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED);
    }
  }


  @Nested
  class WhenCachePopulationHasBeenFinishedSuccessfully {

    @Nested
    class AndJobIdHasBeenProvided {

      @Nested
      class AndFlagToSendEmailsIsTrue {

        @Test
        public void shouldMarkJobAsFinishedSuccessfullyAndSendEmails() {
          // given
          mockFinishedCachingWithResult(CachePopulationResult.ALL_RECORDS_CACHED);
          mockUsersForAccount(ANY_ACCOUNT_ID);

          // when
          chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, ANY_JOB_ID,
              ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID, true);

          // then
          verify(registerJobSupervisor).updateStatus(ANY_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);
          verify(chargeCalculationCompleteEmailSender).send(ANY_EMAIL);
          verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), any(), any(), anyInt(),
              anyBoolean());
        }
      }

      @Nested
      class AndFlagToSendEmailsIsFalse {

        @Test
        public void shouldMarkJobAsFinishedSuccessfullyAndNotSendEmails() {
          // given
          mockFinishedCachingWithResult(CachePopulationResult.ALL_RECORDS_CACHED);

          // when
          chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, ANY_JOB_ID,
              ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID, false);

          // then
          verify(registerJobSupervisor).updateStatus(ANY_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);
          verify(chargeCalculationCompleteEmailSender, never()).send(anyString());
          verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), any(), any(), anyInt(),
              anyBoolean());
        }
      }
    }

    @Nested
    class AndJobIdHasNotBeenProvided {

      @Test
      public void shouldNotDoAnything() {
        // given
        mockFinishedCachingWithResult(CachePopulationResult.ALL_RECORDS_CACHED);

        // when
        chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, null,
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID,
            ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG);

        // then
        verify(chargeCalculationCompleteEmailSender, never()).send(any());
        verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), any(), any(), anyInt(),
            anyBoolean());
      }
    }

    private void mockUsersForAccount(UUID anyAccountId) {
      given(userService.getAllUsersForAccountId(anyAccountId)).willReturn(
          Collections.singletonList(User.builder().email(ANY_EMAIL).build())
      );
    }

    private void mockFinishedCachingWithResult(CachePopulationResult status) {
      given(chargeCalculationService.populateCache(ANY_ACCOUNT_ID, MAX_VEHICLES_TO_PROCESS))
          .willReturn(status);
    }
  }

  @Nested
  class WhenCachePopulationHasFailed {

    @Nested
    class AndJobIdHasBeenProvided {

      @Test
      public void shouldMarkJobAsFinishedSuccessfully() {
        // given
        mockFailedCaching();

        // when
        chargeCalculationJobSupervisor.populateChargeCalculationCache(ANY_ACCOUNT_ID, ANY_JOB_ID,
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID,
            ANY_SHOULD_SEND_EMAILS_UPON_SUCCESSFUL_JOB_COMPLETION_FLAG);

        // then
        verify(registerJobSupervisor).updateStatus(ANY_JOB_ID, RegisterJobStatus.UNKNOWN_FAILURE);
        verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), any(),
            any(), anyInt(), anyBoolean());
        verify(chargeCalculationCompleteEmailSender, never()).send(any());
      }

    }

    private void mockFailedCaching() {
      given(chargeCalculationService.populateCache(ANY_ACCOUNT_ID, MAX_VEHICLES_TO_PROCESS))
          .willReturn(CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION);
    }

  }
}