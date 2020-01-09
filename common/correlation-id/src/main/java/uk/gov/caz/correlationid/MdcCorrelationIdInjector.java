package uk.gov.caz.correlationid;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Class responsible for extracting the value of 'X-Correlation-ID' http header and putting it to
 * {@link MDC}. If the header is missing, {@link MissingCorrelationIdHeaderException} is thrown.
 */
@AllArgsConstructor
@Slf4j
public class MdcCorrelationIdInjector extends HandlerInterceptorAdapter {

  private static final MissingCorrelationIdHeaderException MISSING_CORRELATION_ID_HEADER_EXCEPTION =
      new MissingCorrelationIdHeaderException();

  private final MdcAdapter mdc;

  /**
   * Gets the value of 'X-Correlation-ID' http header and puts it to {@link MDC}. If the header
   * is missing, {@link MissingCorrelationIdHeaderException} is thrown.
   *
   * @throws MissingCorrelationIdHeaderException if 'X-Correlation-ID' http header is absent in
   *     the request.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String correlationId = Optional
        .ofNullable(request.getHeader(X_CORRELATION_ID_HEADER))
        .orElseThrow(() -> MISSING_CORRELATION_ID_HEADER_EXCEPTION);

    log.info("Correlation id for this request is {}", correlationId);
    mdc.put(X_CORRELATION_ID_HEADER, correlationId);
    response.setHeader(X_CORRELATION_ID_HEADER, correlationId);
    return true;
  }

  /**
   * Removes the value which is stored under {@code X_CORRELATION_ID_HEADER} key in {@link MDC}.
   */
  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    mdc.remove(X_CORRELATION_ID_HEADER);
  }

  /**
   * Gets the current value of 'X-Correlation-ID' header from {@link MDC}.
   *
   * @return The current value of 'X-Correlation-ID' header if exists, {@code null} otherwise.
   */
  public static String getCurrentValue() {
    return MDC.get(X_CORRELATION_ID_HEADER);
  }

  @ResponseStatus(value = BAD_REQUEST, reason = "Missing request header 'X-Correlation-ID'")
  public static class MissingCorrelationIdHeaderException extends RuntimeException {

    MissingCorrelationIdHeaderException() {
      super("Missing request header 'X-Correlation-ID'");
    }
  }
}
