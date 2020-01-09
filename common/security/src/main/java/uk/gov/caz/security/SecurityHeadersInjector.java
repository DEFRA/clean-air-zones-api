package uk.gov.caz.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Handlers for pre- and post- request snippets. In this case injects JAQU CAZ common security
 * headers into the response.
 */
public class SecurityHeadersInjector extends HandlerInterceptorAdapter {

  public static final String STRICT_TRANSPORT_SECURITY_HEADER = "Strict-Transport-Security";
  public static final String STRICT_TRANSPORT_SECURITY_VALUE = "max-age=31536000";
  public static final String PRAGMA_HEADER = "Pragma";
  public static final String PRAGMA_HEADER_VALUE = "no-cache";
  public static final String X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";
  public static final String X_CONTENT_TYPE_OPTIONS_VALUE = "nosniff";
  public static final String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
  public static final String X_FRAME_OPTIONS_VALUE = "sameorigin";
  public static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";
  public static final String CONTENT_SECURITY_POLICY_VALUE = "default-src 'self'";
  public static final String CACHE_CONTROL_HEADER = "Cache-Control";
  public static final String CACHE_CONTROL_VALUE = "max-age=31536000, public, s-maxage=31536000";

  /**
   * Allows to manipulate request and response objects before processing request.
   *
   * @param request {@link HttpServletRequest} that is incoming to Spring-Boot.
   * @param response {@link HttpServletResponse} that Spring-Boot will return to the caller.
   * @param handler chosen handler to execute, for type and/or instance evaluation.
   * @return true if the execution chain should proceed with the next interceptor or the handler
   *     itself. Else, DispatcherServlet assumes that this interceptor has already dealt with the
   *     response itself.
   * @throws Exception in case of errors.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    response.setHeader(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE);
    response.setHeader(PRAGMA_HEADER, PRAGMA_HEADER_VALUE);
    response.setHeader(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE);
    response.setHeader(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE);
    response.setHeader(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE);
    response.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE);

    return super.preHandle(request, response, handler);
  }
}
