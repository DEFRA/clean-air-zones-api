package uk.gov.caz.vcc.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration class for request mapping within the application.
 *
 */
@Configuration
public class RequestMappingConfiguration {

  /**
   * Creates required HandlerMapping, to avoid several default HandlerMapping
   * instances being created.
   * @return HandlerMapping instance for the application.
   */
  @Bean
  public HandlerMapping handlerMapping() {
    return new RequestMappingHandlerMapping();
  }

  /**
   * Create required HandlerAdapter, to avoid several default HandlerAdapter
   * instances being created.
   * @return HandlerAdapter instance for the application.
   */
  @Bean
  public HandlerAdapter handlerAdapter() {
    return new RequestMappingHandlerAdapter();
  }
}
