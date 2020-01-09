package uk.gov.caz.async.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
class RequestCallbackTest {

  public static final String ANY_STRING = "any string";
  public static final String ANY_ERROR_MESSAGE = "any error message";

  @Mock
  private AsyncOp asyncOp;

  private RequestCallback requestCallback;

  @Mock
  private Call call;

  @Mock
  private ResponseBody responseBody;

  @BeforeEach
  void setUp() {
    requestCallback = new RequestCallback(asyncOp);
  }

  @Test
  void successfulResponseMarksAsyncOpAsSuccessful() {
    // given
    Response<String> success = Response.success(ANY_STRING);

    // when
    requestCallback.onResponse(call, success);

    // then
    verify(asyncOp, Mockito.times(1)).markCompletedAsSuccessful(HttpStatus.OK, ANY_STRING);
  }

  @Test
  void failureResponseMarksAsyncOpAsFailed() throws IOException {
    // given
    given(responseBody.string()).willReturn(ANY_ERROR_MESSAGE);
    Response<String> failure = Response.error(503, responseBody);

    // when
    requestCallback.onResponse(call, failure);

    // then
    verify(asyncOp, Mockito.times(1))
        .markCompletedAsFailed(HttpStatus.SERVICE_UNAVAILABLE, ANY_ERROR_MESSAGE);
  }

  @Test
  void failureResponseWithInvalidBodyMarksAsyncOpAsFailed() throws IOException {
    // given
    given(responseBody.string()).willThrow(new IOException());
    Response<String> failure = Response.error(503, responseBody);

    // when
    requestCallback.onResponse(call, failure);

    // then
    verify(asyncOp, Mockito.times(1))
        .markCompletedAsFailed(HttpStatus.SERVICE_UNAVAILABLE, "Unable to extract error message");
  }

  @Test
  void seriousCallFailureMarksAsyncOpAsFailed() {
    // given
    Throwable throwable = new Throwable(ANY_ERROR_MESSAGE);

    // when
    requestCallback.onFailure(call, throwable);

    // then
    verify(asyncOp, Mockito.times(1))
        .markCompletedAsFailed(HttpStatus.INTERNAL_SERVER_ERROR, ANY_ERROR_MESSAGE);
  }
}