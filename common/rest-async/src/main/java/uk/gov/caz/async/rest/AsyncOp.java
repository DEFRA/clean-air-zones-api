package uk.gov.caz.async.rest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import retrofit2.Call;


/**
 * Represents asynchronous REST call operation. Created in ready state can be started in async mode
 * and later can be used to wait for response which can be queried.
 */
@Getter(AccessLevel.PACKAGE)
public class AsyncOp<V> {

  /**
   * Async operation identifier. Usable in logs and debugging.
   */
  @Getter(AccessLevel.PUBLIC)
  String identifier;

  /**
   * Wraps lower level Retrofit2 {@link Call} object which does the heavy lifting.
   */
  private Call<V> retrofitCall;

  /**
   * Internal helper that provides functionality to wait for completion.
   */
  private CompletableFuture<Void> completableFuture;

  /**
   * Will hold HTTP Status code of REST call. Initially empty.
   */
  @Getter(AccessLevel.NONE)
  private Optional<HttpStatus> httpStatus;

  /**
   * Will hold result of REST call. Initially empty. Will only be filled when response is
   * successful.
   */
  @Getter(AccessLevel.NONE)
  private Optional<V> result;

  /**
   * Will hold error message if REST call failed. Initially empty. Will only be filled when call
   * finishes with error.
   */
  @Getter(AccessLevel.NONE)
  private Optional<String> error;

  /**
   * Creates {@link AsyncOp} identified by identifier and wrapping Retrofit2 {@link Call} that was
   * obtained by calling REST API method.
   *
   * @param identifier Async operation identifier. Usable in logs and debugging.
   * @param retrofitCall Lower level Retrofit2 {@link Call} object which does the heavy lifting
   *     of REST call operation.
   * @param <V> Type of expected result in case of successful call.
   * @return {@link AsyncOp} object representing asynchronous REST call.
   */
  public static <V> AsyncOp<V> from(@NonNull String identifier, @NonNull Call<V> retrofitCall) {
    return new AsyncOp<>(identifier, retrofitCall);
  }

  /**
   * Creates {@link AsyncOp} identified by identifier and immediately marks it as completed
   * successfully with result value and HttpStatus code.
   *
   * @param identifier Async operation identifier. Usable in logs and debugging.
   * @param httpStatus {@link HttpStatus} holding HTTP status code of REST call.
   * @param result Object with REST call result value.
   * @param <V> Type of expected result.
   * @return {@link AsyncOp} object representing successful asynchronous REST call.
   */
  public static <V> AsyncOp<V> asCompletedAndSuccessful(@NonNull String identifier,
      @NonNull HttpStatus httpStatus, @NonNull V result) {
    AsyncOp<V> asyncOp = new AsyncOp<>(identifier, null);
    asyncOp.reset();
    asyncOp.markCompletedAsSuccessful(httpStatus, result);
    return asyncOp;
  }

  /**
   * Creates {@link AsyncOp} identified by identifier and immediately marks it as failed with error
   * message and HttpStatus code.
   *
   * @param identifier Async operation identifier. Usable in logs and debugging.
   * @param httpStatus {@link HttpStatus} holding HTTP status code of REST call.
   * @param error String with error message.
   * @param <V> Type of expected result.
   * @return {@link AsyncOp} object representing failed asynchronous REST call.
   */
  public static <V> AsyncOp<V> asCompletedAndFailed(@NonNull String identifier,
      @NonNull HttpStatus httpStatus, @NonNull String error) {
    AsyncOp<V> asyncOp = new AsyncOp<>(identifier, null);
    asyncOp.reset();
    asyncOp.markCompletedAsFailed(httpStatus, error);
    return asyncOp;
  }

  /**
   * Constructs new instance of {@link AsyncOp} class.
   *
   * @param identifier Async operation identifier. Usable in logs and debugging.
   * @param retrofitCall Lower level Retrofit2 {@link Call} object which does the heavy lifting
   *     of REST call operation.
   */
  private AsyncOp(String identifier, Call<V> retrofitCall) {
    this.identifier = identifier;
    this.retrofitCall = retrofitCall;
  }

  /**
   * Returns information about error state of REST call operation.
   *
   * @return true if operation finished with error, false if there was success.
   */
  public boolean hasError() {
    return error.isPresent();
  }

  /**
   * Gets {@link HttpStatus} of completed async response. If async call has not finished it will
   * throw {@link IllegalStateException}.
   *
   * @return {@link HttpStatus} of completed async response.
   * @throws IllegalStateException if async call has not finished yet.
   */
  public HttpStatus getHttpStatus() {
    return httpStatus.orElseThrow(
        () -> new IllegalStateException(
            "Async operation did not finish yet. HttpStatus is not available."));
  }

  /**
   * Gets result of completed and successful async response. If async call has not finished it will
   * throw {@link IllegalStateException}.
   *
   * @return result of completed and successful async response.
   * @throws IllegalStateException if async call has not finished yet.
   */
  public V getResult() {
    return result.orElseThrow(() -> new IllegalStateException(
        "Async operation did not finish yet. HttpStatus is not available."));
  }

  /**
   * Gets error of completed and failed async response. If async call has not finished it will throw
   * {@link IllegalStateException}.
   *
   * @return error of completed and failed async response.
   * @throws IllegalStateException if async call has not finished yet.
   */
  public String getError() {
    return error.orElseThrow(() -> new IllegalStateException(
        "Async operation did not finish yet. Error is not available."));
  }

  /**
   * Prepares {@link AsyncOp} for asynchronous run and starts async REST call. This operation may be
   * called multiple times on the same object, however only last call will provide results. If this
   * particular object (async call) is already in-flight it will be cancelled.
   */
  void startAsync() {
    // If this AsyncOp has been created as completed, in opposition to having a need to make a
    // REST call, trying to start it is illegal, hence fast return here.
    if (hasBeenCompletedManually()) {
      return;
    }
    // If startAsync has been called on already running (or finished) REST call then
    // make graceful cancel HTTP operation and cancel any waiting Threads.
    if (completableFuture != null) {
      completableFuture.cancel(true);
      retrofitCall.cancel();
      retrofitCall = retrofitCall.clone();
    }
    reset();
    retrofitCall.enqueue(new RequestCallback<>(this));
  }

  /**
   * Method used to mark this {@link AsyncOp} as finished successfully.
   *
   * @param httpStatus {@link HttpStatus} holding HTTP status code of REST call.
   * @param result Object holding successful response of REST call.
   */
  void markCompletedAsSuccessful(HttpStatus httpStatus, V result) {
    try {
      this.httpStatus = Optional.of(httpStatus);
      this.result = Optional.of(result);
      this.error = Optional.empty();
    } finally {
      completableFuture.complete(null);
    }
  }

  /**
   * Method used to mark this {@link AsyncOp} as finished, but with failure.
   *
   * @param httpStatus {@link HttpStatus} holding HTTP status code of REST call.
   * @param errorMessage Failure details.
   */
  void markCompletedAsFailed(HttpStatus httpStatus, String errorMessage) {
    try {
      this.httpStatus = Optional.of(httpStatus);
      result = Optional.empty();
      error = Optional.of(errorMessage);
    } finally {
      completableFuture.complete(null);
    }
  }

  /**
   * Resets internal state variables to be empty and ready for the REST call.
   */
  private void reset() {
    result = Optional.empty();
    httpStatus = Optional.empty();
    error = Optional.empty();
    completableFuture = new CompletableFuture<>();
  }

  /**
   * Returns whether this AsyncOp has been started or not (so async operation is ongoing or has
   * finished).
   */
  public boolean hasBeenStarted() {
    return hasBeenCompletedManually() || retrofitCall.isExecuted();
  }

  /**
   * Returns whether this AsyncOp has been created by completed manually. If so, such AsyncOp
   * behaves differently and must skip some internal operations.
   */
  private boolean hasBeenCompletedManually() {
    return retrofitCall == null;
  }
}
