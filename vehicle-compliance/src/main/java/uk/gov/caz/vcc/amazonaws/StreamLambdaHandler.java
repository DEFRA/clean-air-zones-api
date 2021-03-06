package uk.gov.caz.vcc.amazonaws;

import static uk.gov.caz.awslambda.AwsHelpers.splitToArray;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import uk.gov.caz.vcc.Application;
import uk.gov.caz.vcc.util.VrnLogFormatter;

/**
 * Primary AWS Lambda handler implementation for serving REST API requests.
 *
 */
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
      
      // Change log formatter to remove vrns from urls in logs.
      handler.setLogFormatter(new VrnLogFormatter<>());
      
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
    byte[] inputBytes = StreamUtils.copyToByteArray(inputStream);
    if (isWarmupRequest(toString(inputBytes))) {
      delayToAllowAnotherLambdaInstanceWarming(handler, context);
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      log.info("Processing API request");
      LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
      handler.proxyStream(toInputStream(inputBytes), outputStream, context);
    }
  }

  /**
   * Converts byte array to {@link InputStream}.
   *
   * @param inputBytes Input byte array.
   * @return {@link InputStream} over byte array.
   * @throws IOException When unable to convert.
   */
  private InputStream toInputStream(byte[] inputBytes) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(inputBytes)) {
      return inputStream;
    }
  }

  /**
   * Converts {@code inputBytes} to an UTF-8 encoded string.
   */
  private String toString(byte[] inputBytes) {
    return new String(inputBytes, StandardCharsets.UTF_8);
  }

  /**
   * Delay lambda response if the container is new and cold to allow subsequent keep-warm requests
   * to be routed to a different lambda container.
   *
   * @throws IOException when it is impossible to pause the thread
   */
  private void delayToAllowAnotherLambdaInstanceWarming(
      SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Context context) throws IOException {
    try {
      if (LambdaContainerStats.getLatestRequestTime() == null) {
        long initStartTime = Instant.now().toEpochMilli();
        AwsProxyRequest awsProxyRequest = buildHealthCheckRequest();
        // force spring initialation to finish
        handler.proxy(awsProxyRequest, context);
        long initEndTime = Instant.now().toEpochMilli();
        long initDuration = initEndTime - initStartTime;
        long sleepDuration = Long.parseLong(Optional.ofNullable(
            System.getenv("thundra_lambda_warmup_warmupSleepDuration"))
            .orElse("100"));
        if (sleepDuration > initDuration) {
          log.info(String.format("Container %s go to sleep for %f seconds",
              LambdaContainerStats.getInstanceId(),
              (double) (sleepDuration - initDuration) / 1000));
          Thread.sleep(sleepDuration - initDuration);
        }
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Build a virtual health check request.
   */
  private AwsProxyRequest buildHealthCheckRequest() {
    return new AwsProxyRequestBuilder("/virtual/health/check", "GET")
        .header("X-Correlation-ID",UUID.randomUUID().toString())
        .header("Content-Type","application/json")
        .nullBody()
        .build();
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

  /**
   * Contain information about the lambda container.
   */
  static class LambdaContainerStats {

    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static LocalDateTime latestRequestTime;

    private LambdaContainerStats() {
      throw new IllegalStateException("Utility class");
    }

    /**
     * Set the time that the container serves a request.
     */
    public static void setLatestRequestTime(LocalDateTime dt) {
      latestRequestTime = dt;
    }

    /**
     * Get the time that the container last served a request.
     */
    public static LocalDateTime getLatestRequestTime() {
      return latestRequestTime;
    }

    /**
     * Get container instanceId.
     */
    public static String getInstanceId() {
      return INSTANCE_ID;
    }

    /**
     * Get the container stats.
     *
     * @return a string that contains lambda container Id and (optionally) the time that the
     *     container last serve a request.
     */
    public static String getStats() {
      try {
        ObjectMapper obj = new ObjectMapper();
        Map<String, String> retVal = new HashMap<>();
        retVal.put("instanceId", INSTANCE_ID);
        if (latestRequestTime != null) {
          retVal.put("latestRequestTime", latestRequestTime.format(formatter));
        }
        return obj.writeValueAsString(retVal);
      } catch (JsonProcessingException ex) {
        return String.format("\"instanceId\": \"%s\"", INSTANCE_ID);
      }
    }
  }
}