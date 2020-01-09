package uk.gov.caz.logger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.amazonaws.services.lambda.runtime.LambdaRuntimeInternal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = AwsLambdaLoggerConfigurer.class)
public class AwsLambdaLoggerConfigurerTest {

  @Autowired
  private AwsLambdaLoggerConfigurer awsLambdaLoggerConfigurer;

  @Test
  public void shouldConfigureAwsLoggerOnStartup() {
    assertThat(LambdaRuntimeInternal.getUseLog4jAppender()).isTrue();
  }
}
