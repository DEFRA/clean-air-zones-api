package uk.gov.caz.async.rest;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncRestServiceTest {

  @Mock
  private AsyncOp asyncOp1;
  @Mock
  private CompletableFuture<Void> completableFuture1;

  @Mock
  private AsyncOp asyncOp2;
  @Mock
  private CompletableFuture<Void> completableFuture2;

  private AsyncRestService asyncRestService;

  @BeforeEach
  void setUp() {
    completableFuture1 = new CompletableFuture<>();
    lenient().when(asyncOp1.getCompletableFuture()).thenReturn(completableFuture1);

    completableFuture2 = new CompletableFuture<>();
    lenient().when(asyncOp2.getCompletableFuture()).thenReturn(completableFuture2);

    asyncRestService = new AsyncRestService();
  }

  @Test
  void testStaringAsyncOps() {
    // when
    asyncRestService.start(asyncOp1);
    asyncRestService.startAll(asyncOp1, asyncOp2);
    asyncRestService.startAll(newArrayList(asyncOp1, asyncOp2));

    // then
    verify(asyncOp1, times(3)).startAsync();
    verify(asyncOp2, times(2)).startAsync();
  }

  @Test
  void testWaitingForSingleAsyncOpThatFinishesInTime() {
    // given
    markOp1AsStarted();
    completeOp1In(1, TimeUnit.SECONDS);

    // when
    asyncRestService.await(asyncOp1, 5, TimeUnit.SECONDS);

    // then
    // No AsyncCallbackException or any other Exception was thrown
  }

  @Test
  void testWaitingForMultipleAsyncOpsThatFinishInTime() {
    // given
    markOp1AsStarted();
    markOp2AsStarted();
    completeOp1AndOp2In(1, TimeUnit.SECONDS);

    // when
    asyncRestService.awaitAll(newArrayList(asyncOp1, asyncOp2), 5, TimeUnit.SECONDS);

    // then
    // No AsyncCallbackException or any other Exception was thrown
  }

  @Test
  void testStartingAndWaitingForAllMethod() {
    // given
    markOp1AsStarted();
    markOp2AsStarted();
    completeOp1AndOp2In(1, TimeUnit.SECONDS);

    // when
    asyncRestService.startAndAwaitAll(newArrayList(asyncOp1, asyncOp2), 5, TimeUnit.SECONDS);

    // then
    // No AsyncCallbackException or any other Exception was thrown
    verify(asyncOp1).startAsync();
    verify(asyncOp2).startAsync();
  }

  @Test
  void tryingToWaitForNotStartedAsyncJobThrowsIllegalStateException() {
    // given
    markOp1AsNotStarted();

    // when
    Throwable throwable = catchThrowable(
        () -> asyncRestService.await(asyncOp1, 5, TimeUnit.SECONDS));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
    assertThat(throwable)
        .hasMessage("AsyncOp null has not been started. Please make sure you call 'startAsync'.");
  }

  @Test
  void waitingForAsyncJobThatWontFinishInTimeThrowsAsyncCallExceptionWithTimeoutMessage() {
    // given AsyncOp will complete in 2 seconds
    markOp1AsStarted();
    completeOp1In(2, TimeUnit.SECONDS);

    // when waiting for 1 second
    Throwable throwable = catchThrowable(
        () -> asyncRestService.await(asyncOp1, 1, TimeUnit.SECONDS));

    // then
    assertThat(throwable).isInstanceOf(AsyncCallException.class);
    assertThat(throwable).hasMessage("Timeout");
  }

  @Test
  void interruptionOrCancellationWhenWaitingForAsyncOpFinishThrowsAsyncCallException() {
    // given
    markOp1AsStarted();
    cancelOp1In(1, TimeUnit.SECONDS);

    // when
    Throwable throwable = catchThrowable(
        () -> asyncRestService.await(asyncOp1, 5, TimeUnit.SECONDS));

    // then
    assertThat(throwable).isInstanceOf(AsyncCallException.class);
    assertThat(throwable).hasMessage(
        "java.util.concurrent.ExecutionException: java.util.concurrent.CancellationException");
  }

  private void markOp1AsStarted() {
    given(asyncOp1.hasBeenStarted()).willReturn(true);
  }

  private void markOp1AsNotStarted() {
    given(asyncOp1.hasBeenStarted()).willReturn(false);
  }

  private void markOp2AsStarted() {
    given(asyncOp2.hasBeenStarted()).willReturn(true);
  }

  private void completeOp1In(int howLong, TimeUnit timeUnit) {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    executor.schedule(() -> completableFuture1.complete(null), howLong, timeUnit);
  }

  private void cancelOp1In(int howLong, TimeUnit timeUnit) {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    executor.schedule(() -> completableFuture1.cancel(true), howLong, timeUnit);
  }

  private void completeOp1AndOp2In(int howLong, TimeUnit timeUnit) {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    executor.schedule(() -> {
      completableFuture1.complete(null);
      completableFuture2.complete(null);
    }, howLong, timeUnit);
  }
}