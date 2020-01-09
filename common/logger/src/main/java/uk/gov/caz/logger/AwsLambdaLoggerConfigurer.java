package uk.gov.caz.logger;

import com.amazonaws.services.lambda.runtime.LambdaRuntimeInternal;
import javax.annotation.PostConstruct;

public class AwsLambdaLoggerConfigurer {

  /**
   * Configures log4j appender on AWS. It effectively enables MDC support.
   */
  @PostConstruct
  public void setup() {
    LambdaRuntimeInternal.setUseLog4jAppender(true);
  }
}
