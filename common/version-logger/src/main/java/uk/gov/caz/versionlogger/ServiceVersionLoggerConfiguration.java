package uk.gov.caz.versionlogger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceVersionLoggerConfiguration {

  @Bean
  public ServiceVersionLogger serviceVersionLogger(
      @Autowired(required = false) GitProperties gitProperties) {
    return new ServiceVersionLogger(gitProperties);
  }
}
