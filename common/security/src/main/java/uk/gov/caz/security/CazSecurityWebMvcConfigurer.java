package uk.gov.caz.security;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows to connect {@link SecurityHeadersInjector} to request-response processing flow.
 */
public class CazSecurityWebMvcConfigurer implements WebMvcConfigurer {

  /**
   * Add Spring MVC lifecycle interceptors for pre- and post-processing of controller method
   * invocations. Interceptors can be registered to apply to all requests or be limited to a subset
   * of URL patterns.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new SecurityHeadersInjector());
  }
}
