package uk.gov.caz.correlationid;

import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {

  @Bean
  public CorrelationIdWebMvcConfigurer correlationIdWebMvcConfigurer(
      CorrelationIdProperties correlationIdProperties, MdcAdapter mdcAdapter) {
    return new CorrelationIdWebMvcConfigurer(correlationIdProperties, mdcAdapter);
  }

  @Bean
  public CorrelationIdProperties correlationIdProperties() {
    return new CorrelationIdProperties();
  }

  @Bean
  public MdcAdapter mdcAdapter() {
    return new MdcAdapter();
  }
}
