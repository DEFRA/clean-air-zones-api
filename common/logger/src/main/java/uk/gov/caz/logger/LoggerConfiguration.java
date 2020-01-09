package uk.gov.caz.logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggerConfiguration {

  @Bean
  public AwsLambdaLoggerConfigurer awsLambdaLoggerConfigurer() {
    return new AwsLambdaLoggerConfigurer();
  }
}
