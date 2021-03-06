package uk.gov.caz.whitelist.amazonaws;

import static uk.gov.caz.awslambda.AwsHelpers.splitToArray;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import uk.gov.caz.whitelist.Application;
import uk.gov.caz.whitelist.dto.LambdaContainerStats;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

  private static final String KEEP_WARM_ACTION = "keep-warm";
  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  static {
    try {
      log.info("Initializing lambda handler");
      
      String listOfActiveSpringProfiles =
          System.getenv("SPRING_PROFILES_ACTIVE");
      
      if (listOfActiveSpringProfiles != null) {
        handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
            .defaultProxy()
            .asyncInit()
            .springBootApplication(Application.class)
            .profiles(splitToArray(listOfActiveSpringProfiles))
            .buildAndInitialize();
      } else {
        handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
            .defaultProxy()
            .asyncInit()
            .springBootApplication(Application.class)
            .buildAndInitialize();
      }
      
      log.info("Lambda handler initialization finished");
    } catch (ContainerInitializationException e) {
      // If we fail here. We re-throw the exception to force another cold start
      log.info("Initialization of lambda failed");
      throw new IllegalStateException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());    
    if (isWarmupRequest(input)) {
      delayToAllowAnotherLambdaInstanceWarming();
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
      handler.proxyStream(new ByteArrayInputStream(input.getBytes()), outputStream, context);
    }
  }

  /**
   * Delay lambda response if the container is new and cold
   * to allow subsequent keep-warm requests to be routed to a different lambda container.
   *
   * @throws IOException when it is impossible to pause the thread
   */
  private void delayToAllowAnotherLambdaInstanceWarming() throws IOException {
    try {
      if (LambdaContainerStats.getLatestRequestTime() == null) {
        int sleepDuration = Integer.parseInt(Optional.ofNullable(
                                  System.getenv("thundra_lambda_warmup_warmupSleepDuration"))
                                  .orElse("100"));
        log.info(String.format("Container %s go to sleep for %f seconds",
            LambdaContainerStats.getInstanceId(),
            (double)sleepDuration / 1000));
        Thread.sleep(sleepDuration);
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Determine if the incoming request is a keep-warm one.
   *
   * @param action the request under examination.
   * @return true if the incoming request is a keep-warm one otherwise false.
   */
  private boolean isWarmupRequest(String action) {
    boolean isWarmupRequest = action.contains(KEEP_WARM_ACTION);

    if (isWarmupRequest) {
      log.info("Received lambda warmup request");
    }
    
    return isWarmupRequest;
  }
}
