package uk.gov.caz.logger;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.LambdaRuntimeInternal;
import org.junit.jupiter.api.Test;

public class AwsLambdaLoggerConfigurerUnitTest {

  @Test
  public void shouldConfigureAwsLogger() {
    //when
    new LoggerConfiguration().awsLambdaLoggerConfigurer().setup();

    //then
    assertThat(LambdaRuntimeInternal.getUseLog4jAppender()).isTrue();
  }
}
