package uk.gov.caz.async.rest;

import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides convenient methods to start and wait for asynchronous REST calls.
 */
@Slf4j
public class AsyncRestService {

  /**
   * Starts single {@link AsyncOp}. Basically will do async REST call using selected REST API
   * method.
   *
   * @param asyncOp {@link AsyncOp} object representing asynchronous REST call.
   */
  public void start(AsyncOp asyncOp) {
    startAll(singletonList(asyncOp));
  }

  /**
   * Starts multiple {@link AsyncOp} operations. Basically will do async REST calls using selected
   * REST API methods.
   *
   * @param asyncOps Vararg array of {@link AsyncOp} objects representing asynchronous REST
   *     calls.
   */
  public void startAll(AsyncOp... asyncOps) {
    startAll(Arrays.asList(asyncOps));
  }

  /**
   * Starts multiple {@link AsyncOp} operations. Basically will do async REST calls using selected
   * REST API methods.
   *
   * @param asyncOps List of {@link AsyncOp} objects representing asynchronous REST calls.
   */
  public void startAll(List<? extends AsyncOp> asyncOps) {
    log.info("Starting {} async requests", asyncOps.size());
    asyncOps.forEach(AsyncOp::startAsync);
  }

  /**
   * Given single {@link AsyncOp} will wait for desired time until it completes. If operation is
   * already completed or completes before desired time will return quicker.
   *
   * @param asyncOp {@link AsyncOp} object representing asynchronous REST call.
   * @param forHowLong Amount of maximum time to wait until async call completes.
   * @param timeUnit {@link TimeUnit} giving meaning to {@code forHowLong} parameter.
   * @throws AsyncCallException if calls did not finish in desired time or in case of
   *     interruption.
   */
  public void await(AsyncOp asyncOp, long forHowLong, TimeUnit timeUnit) {
    validateIfStarted(asyncOp);
    awaitAll(singletonList(asyncOp), forHowLong, timeUnit);
  }

  /**
   * Given multiple {@link AsyncOp} operations will wait for desired time until ALL of them
   * complete. If they are already completed or complete before desired time will return quicker.
   *
   * @param asyncOps List of {@link AsyncOp} objects representing asynchronous REST calls.
   * @param forHowLong Amount of maximum time to wait until async call completes.
   * @param timeUnit {@link TimeUnit} giving meaning to {@code forHowLong} parameter.
   * @throws AsyncCallException if calls did not finish in desired time or in case of
   *     interruption.
   */
  public void awaitAll(List<? extends AsyncOp> asyncOps, long forHowLong, TimeUnit timeUnit) {
    try {
      CompletableFuture
          .allOf(getCompletableFuturesFrom(asyncOps))
          .get(forHowLong, timeUnit);
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted", e);
      throw new AsyncCallException(e);
    } catch (TimeoutException timeoutException) {
      log.warn("Timeout exception for ids {}",
          asyncOps.stream().map(AsyncOp::getIdentifier).collect(Collectors.toList()));
      throw new AsyncCallException("Timeout");
    }
  }

  /**
   * Given multiple {@link AsyncOp} operations will start them and then wait for desired time until
   * ALL of them complete. If they are already completed or complete before desired time will return
   * quicker.
   *
   * @param asyncOps List of {@link AsyncOp} objects representing asynchronous REST calls.
   * @param forHowLong Amount of maximum time to wait until async call completes.
   * @param timeUnit {@link TimeUnit} giving meaning to {@code forHowLong} parameter.
   * @throws AsyncCallException if calls did not finish in desired time or in case of
   *     interruption.
   */
  public void startAndAwaitAll(List<? extends AsyncOp> asyncOps, long forHowLong,
      TimeUnit timeUnit) {
    startAll(asyncOps);
    awaitAll(asyncOps, forHowLong, timeUnit);
  }

  /**
   * Given List of {@link AsyncOp} will validate if every op has been started and extracts {@link
   * CompletableFuture} into an Array. Such array can be used as wait-for-multiple operation.
   */
  private CompletableFuture[] getCompletableFuturesFrom(List<? extends AsyncOp> asyncOps) {
    return asyncOps.stream()
        .map(validateIfStartedAndExtractCompletableFuture())
        .toArray(CompletableFuture[]::new);
  }

  /**
   * Verifies if {@link AsyncOp} has been started. If not throws {@link IllegalStateException}.
   *
   * @param asyncOp {@link AsyncOp} object representing asynchronous REST call.
   * @throws IllegalStateException if {@link AsyncOp} has not been started.
   */
  private void validateIfStarted(AsyncOp asyncOp) {
    if (!asyncOp.hasBeenStarted()) {
      throw new IllegalStateException("AsyncOp " + asyncOp.getIdentifier()
          + " has not been started. Please make sure you call 'startAsync'.");
    }
  }

  /**
   * Validates if {@link AsyncOp} has been started and extracts its {@link CompletableFuture} for
   * further processing.
   */
  private Function<AsyncOp, CompletableFuture> validateIfStartedAndExtractCompletableFuture() {
    return asyncOp -> {
      validateIfStarted(asyncOp);
      return asyncOp.getCompletableFuture();
    };
  }
}
