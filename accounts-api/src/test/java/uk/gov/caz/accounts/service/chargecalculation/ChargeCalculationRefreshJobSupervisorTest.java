package uk.gov.caz.accounts.service.chargecalculation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.service.ChargeCalculationService;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;

@ExtendWith(MockitoExtension.class)
class ChargeCalculationRefreshJobSupervisorTest {

  private static final UUID ANY_CORRELATION_ID = UUID
      .fromString("d69e2371-3020-4b2b-8d4e-9b4b2f0f7cd3");
  private static final int ANY_INVOCATION_NUMBER = 4;

  private static final int CACHE_REFRESH_DAYS = 2;
  private static final int MAX_INVOCATION_COUNT = 10;
  private static final int MAX_VEHICLES_TO_PROCESS = 20;

  @Mock
  private ChargeCalculationService chargeCalculationService;
  @Mock
  private AsyncChargeCalculationRefreshStarter asyncChargeCalculationRefreshStarter;

  private ChargeCalculationRefreshJobSupervisor chargeCalculationRefreshJobSupervisor;

  @BeforeEach
  public void setUp() {
    chargeCalculationRefreshJobSupervisor = new ChargeCalculationRefreshJobSupervisor(
        chargeCalculationService, asyncChargeCalculationRefreshStarter, CACHE_REFRESH_DAYS,
        MAX_INVOCATION_COUNT, MAX_VEHICLES_TO_PROCESS);
  }

  @Nested
  class WhenCacheRefreshHasNotBeenFinished {

    @Nested
    class AndInvocationNumberHasNotBeenReached {

      @Test
      public void shouldInvokeAnotherLambdaToProcessNextBatch() {
        // given
        mockNotFinishedCaching();

        // when
        chargeCalculationRefreshJobSupervisor.refreshChargeCalculationCache(
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID);

        // then
        verify(asyncChargeCalculationRefreshStarter).fireAndForget(
            ANY_CORRELATION_ID, ANY_INVOCATION_NUMBER + 1);
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
        chargeCalculationRefreshJobSupervisor.refreshChargeCalculationCache(
            invocationNumber, ANY_CORRELATION_ID);

        // then
        verify(asyncChargeCalculationRefreshStarter, never())
            .fireAndForget(any(UUID.class), anyInt());
      }
    }

    private void mockNotFinishedCaching() {
      given(chargeCalculationService.refreshCache(MAX_VEHICLES_TO_PROCESS, CACHE_REFRESH_DAYS))
          .willReturn(CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED);
    }
  }


  @Nested
  class WhenCachePopulationHasBeenFinished {

    @Nested
    class Successfully {

      @Test
      public void shouldMarkJobAsFinishedSuccessfullyAndSendEmails() {
        // given
        mockFinishedCachingWithResult(CachePopulationResult.ALL_RECORDS_CACHED);

        // when
        chargeCalculationRefreshJobSupervisor.refreshChargeCalculationCache(
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID);

        // then
        verify(asyncChargeCalculationRefreshStarter, never())
            .fireAndForget(any(UUID.class), anyInt());
      }
    }

    @Nested
    class Failed {

      @Test
      public void shouldMarkJobAsFinishedSuccessfullyAndSendEmails() {
        // given
        mockFinishedCachingWithResult(CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION);

        // when
        chargeCalculationRefreshJobSupervisor.refreshChargeCalculationCache(
            ANY_INVOCATION_NUMBER, ANY_CORRELATION_ID);

        // then
        verify(asyncChargeCalculationRefreshStarter, never())
            .fireAndForget(any(UUID.class), anyInt());
      }
    }

    private void mockFinishedCachingWithResult(CachePopulationResult status) {
      given(chargeCalculationService.refreshCache(MAX_VEHICLES_TO_PROCESS, CACHE_REFRESH_DAYS))
          .willReturn(status);
    }
  }
}