package uk.gov.caz.async.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to create AsyncRestService.
 */
@Configuration
public class AsyncRestConfiguration {

  /**
   * Method to create AsyncRestService bean.
   *
   * @return {@link AsyncRestService} instance.
   */
  @Bean
  public AsyncRestService asyncRestService() {
    return new AsyncRestService();
  }
}
