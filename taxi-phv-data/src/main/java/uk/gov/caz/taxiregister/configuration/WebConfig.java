package uk.gov.caz.taxiregister.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.caz.taxiregister.tasks.AnyTaskEnabled;

@Configuration
@ConditionalOnMissingBean(value = AnyTaskEnabled.class)
public class WebConfig implements WebMvcConfigurer {

  /**
   * Global configuration for content negotiation defaults.
   */
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }
}