package uk.gov.caz.async.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import retrofit2.Call;

@ExtendWith(MockitoExtension.class)
class AsyncOpTest {

  @Mock
  private Call<String> call;

  private AsyncOp<String> asyncOp;

  @BeforeEach
  void setup() {
    asyncOp = AsyncOp.from("AsyncOp", call);
  }

  @Test
  void preconditionValidations() {
    Throwable throwable = catchThrowable(() -> AsyncOp.from(null, call));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(() -> AsyncOp.from("AsyncOp", null));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndSuccessful(null, HttpStatus.OK, new Object()));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndSuccessful("AsyncOp", null, new Object()));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndSuccessful("AsyncOp", HttpStatus.OK, null));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndFailed(null, HttpStatus.NOT_FOUND, "Error"));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndFailed("AsyncOp", null, "Error"));
    assertThat(throwable).isInstanceOf(NullPointerException.class);

    throwable = catchThrowable(
        () -> AsyncOp.asCompletedAndFailed("AsyncOp", HttpStatus.NOT_FOUND, null));
    assertThat(throwable).isInstanceOf(NullPointerException.class);
  }

  @Test
  void startAsyncWhenNotRunBefore() {
    // when
    asyncOp.startAsync();

    // then
    assertThat(asyncOp.getIdentifier()).isEqualTo("AsyncOp");
    assertThatStateIsFreshlyStarted();
    verify(call).enqueue(any(RequestCallback.class));
  }

  @Test
  void startAsyncWhenAlreadyRun() {
    // given
    given(call.clone()).willReturn(call);

    // when
    asyncOp.startAsync();
    asyncOp.startAsync();

    // then
    assertThatStateIsFreshlyStarted();
    verify(call).cancel();

    verify(call, times(2)).enqueue(any(RequestCallback.class));
  }

  @Test
  void hasBeenStartedDependsOnStateOfRetrofitCall() {
    // given
    given(call.isExecuted()).willReturn(true);

    // then
    assertThat(asyncOp.hasBeenStarted()).isTrue();

    // given
    given(call.isExecuted()).willReturn(false);

    // then
    assertThat(asyncOp.hasBeenStarted()).isFalse();

    // given
    AsyncOp<String> asyncOpManuallyCompleted = AsyncOp
        .asCompletedAndSuccessful("OK", HttpStatus.OK, "OK");

    // then
    assertThat(asyncOpManuallyCompleted.hasBeenStarted()).isTrue();
  }

  @Test
  void markCompletedAsSuccessful() {
    // when
    asyncOp.startAsync();
    asyncOp.markCompletedAsSuccessful(HttpStatus.OK, "Success");

    // then
    assertThat(asyncOp.hasError()).isFalse();
    assertThat(asyncOp.getHttpStatus()).isEqualTo(HttpStatus.OK);
    assertThat(asyncOp.getResult()).isEqualTo("Success");
    assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();
    Throwable throwable = catchThrowable(() -> asyncOp.getError());
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void markCompletedAsFailed() {
    // when
    asyncOp.startAsync();
    asyncOp.markCompletedAsFailed(HttpStatus.INTERNAL_SERVER_ERROR, "Error");

    // then
    assertThat(asyncOp.hasError()).isTrue();
    assertThat(asyncOp.getError()).isEqualTo("Error");
    assertThat(asyncOp.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();
    Throwable throwable = catchThrowable(() -> asyncOp.getResult());
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testGetterForRetrofitCall() {
    // when
    Call<String> retrofitCall = asyncOp.getRetrofitCall();

    // then
    assertThat(retrofitCall).isSameAs(call);
  }

  @Nested
  class CreatedAsCompleted {

    @Test
    void andSuccessful() {
      // when
      AsyncOp<String> asyncOp = AsyncOp.asCompletedAndSuccessful("AsyncOp", HttpStatus.OK, "OK");

      // then
      assertThat(asyncOp).isNotNull();
      assertThat(asyncOp.getHttpStatus()).isEqualTo(HttpStatus.OK);
      assertThat(asyncOp.hasError()).isFalse();
      assertThat(asyncOp.getResult()).isEqualTo("OK");
      assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();
    }

    @Test
    void andFailed() {
      // when
      AsyncOp<String> asyncOp = AsyncOp
          .asCompletedAndFailed("AsyncOp", HttpStatus.NOT_FOUND, "NOK");

      // then
      assertThat(asyncOp).isNotNull();
      assertThat(asyncOp.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(asyncOp.hasError()).isTrue();
      assertThat(asyncOp.getError()).isEqualTo("NOK");
      assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();
    }

    @Test
    void shouldBeAbleToBeStartedManyTimesWithoutProblemsOrSideEffects() {
      // given
      AsyncOp<String> asyncOp = AsyncOp.asCompletedAndSuccessful("AsyncOp", HttpStatus.OK, "OK");

      // when
      asyncOp.startAsync();
      asyncOp.startAsync();

      // then
      // No exceptions should be thrown
      assertThat(asyncOp.hasBeenStarted()).isTrue();
      assertThat(asyncOp.hasError()).isFalse();
    }

    @Test
    void shouldImmediatelyReturnFromWait() {
      // given
      AsyncOp<String> asyncOp = AsyncOp.asCompletedAndSuccessful("AsyncOp", HttpStatus.OK, "OK");

      // then
      assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();

      // when
      asyncOp.startAsync();

      // then
      assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();

      // when
      asyncOp.startAsync();

      // then
      assertThat(asyncOp.getCompletableFuture().isDone()).isTrue();
    }
  }

  private void assertThatStateIsFreshlyStarted() {
    assertThat(asyncOp.getCompletableFuture()).isNotNull();

    Throwable throwable = catchThrowable(() -> asyncOp.getResult());
    assertThat(throwable).isInstanceOf(IllegalStateException.class);

    throwable = catchThrowable(() -> asyncOp.getHttpStatus());
    assertThat(throwable).isInstanceOf(IllegalStateException.class);

    throwable = catchThrowable(() -> asyncOp.getError());
    assertThat(throwable).isInstanceOf(IllegalStateException.class);

    assertThat(asyncOp.hasError()).isFalse();
  }
}