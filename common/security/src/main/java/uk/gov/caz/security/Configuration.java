package uk.gov.caz.security;

import org.springframework.context.annotation.Bean;

/**
 * Spring-Boot configuration that wires everything for JAQU CAZ common security features.
 */
@org.springframework.context.annotation.Configuration
public class Configuration {

  /**
   * Creates and initializes {@link CazSecurityWebMvcConfigurer} bean.
   *
   * @return New instance of {@link CazSecurityWebMvcConfigurer} bean.
   */
  @Bean
  public CazSecurityWebMvcConfigurer cazSecurityWebMvcConfigurer() {
    return new CazSecurityWebMvcConfigurer();
  }
}
