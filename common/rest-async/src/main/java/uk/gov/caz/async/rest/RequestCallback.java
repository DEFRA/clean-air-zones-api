package uk.gov.caz.async.rest;

import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Request callback that will be called when async REST call finishes, either with success of
 * failure.
 */
@AllArgsConstructor
@Slf4j
class RequestCallback<V> implements Callback<V> {

  protected static final String ERROR_MESSAGE = "Unable to extract error message";
  private AsyncOp<V> asyncOp;

  @Override
  public void onResponse(Call<V> call, Response<V> response) {
    log.info("Got response for async operation");
    HttpStatus httpStatus = HttpStatus.resolve(response.code());
    if (response.isSuccessful()) {
      log.info("Response is successful with HTTP status {}", httpStatus);
      asyncOp.markCompletedAsSuccessful(httpStatus, response.body());
    } else {
      String errorBody = extractErrorMessage(response);
      log.warn("Failure response: code: {}, error: {}", response.code(), errorBody);
      asyncOp.markCompletedAsFailed(httpStatus, errorBody);
    }
  }

  @Override
  public void onFailure(Call<V> call, Throwable throwable) {
    log.warn("Unable to make HTTP call for async operation");
    asyncOp.markCompletedAsFailed(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
  }

  /**
   * Helper method to extract error message.
   *
   * @param response {@link Response}
   * @return value of error message
   */
  private String extractErrorMessage(Response<V> response) {
    try {
      return response.errorBody().string();
    } catch (IOException e) {
      log.warn("Unable to extract error message from response ", e);
      return ERROR_MESSAGE;
    }
  }
}