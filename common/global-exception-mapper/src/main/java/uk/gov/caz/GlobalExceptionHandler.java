package uk.gov.caz;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Catches all uncaught exceptions and maps them to proper HTTP responses required by JAQU
 * framework.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  static final String ERROR_MESSAGE = "Cannot process request";

  /**
   * Handler of all uncaught exceptions that maps them to Http response.
   *
   * @param ex Uncaught exception.
   * @param request {@link WebRequest} that causes this exception.
   * @return Error response with proper Http status and message.
   */
  @ExceptionHandler({
      Exception.class,
  })
  public ResponseEntity<ErrorResponse> handleCriticalException(Exception ex, WebRequest request) {
    if (ex instanceof ResponseStatusException) {
      log.error("Got ResponseStatusException during processing request", ex);
      return handleResponseStatusException((ResponseStatusException) ex);
    }

    ResponseStatus responseStatus = findMergedAnnotation(ex.getClass(), ResponseStatus.class);
    if (responseStatus != null) {
      log.error("Got Exception with ResponseStatus metadata during processing request", ex);
      String errorMessage = getErrorMessage(ex, responseStatus);
      return handle(new ErrorResponse(responseStatus.code().value(), errorMessage),
          responseStatus.value());
    }

    log.error("Got critical exception during processing request", ex);
    ErrorResponse errorResponse = new ErrorResponse(INTERNAL_SERVER_ERROR.value(),
        ERROR_MESSAGE);
    return handle(errorResponse, INTERNAL_SERVER_ERROR);
  }

  private String getErrorMessage(Exception exception, ResponseStatus responseStatus) {
    if (!StringUtils.isEmpty(responseStatus.reason())) {
      return responseStatus.reason();
    }

    if (exception instanceof ApplicationRuntimeException) {
      return ((ApplicationRuntimeException) exception).message();
    } else {
      return ERROR_MESSAGE;
    }
  }

  /**
   * Handles Spring 5.x+ {@link ResponseStatusException} exception.
   *
   * @param ex {@link ResponseStatusException} that should be mapped.
   * @return Error response with proper HTTP error code and message.
   */
  private ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
    return new ResponseEntity<>(ErrorResponse.from(ex.getStatus().value(), ex.getReason()),
        ex.getStatus());
  }

  /**
   * Handles exception annotated with {@link ResponseStatus} annotation.
   *
   * @param errorResponse {@link ErrorResponse} that should be marshalled as response body.
   * @param httpStatus HTTP error code that should be returned to the caller.
   * @return Error response with proper HTTP error code and message.
   */
  private ResponseEntity<ErrorResponse> handle(ErrorResponse errorResponse, HttpStatus httpStatus) {
    return new ResponseEntity<>(errorResponse, httpStatus);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    log.error(ex.toString());

    return super.handleExceptionInternal(ex, body, headers, status, request);
  }
}
