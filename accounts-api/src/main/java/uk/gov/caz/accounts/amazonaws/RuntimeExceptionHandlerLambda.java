package uk.gov.caz.accounts.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.accounts.Application;
import uk.gov.caz.accounts.dto.CloudWatchDataMessage;
import uk.gov.caz.accounts.dto.CloudWatchDataMessage.LogEvent;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.ValidationError;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;
import uk.gov.caz.awslambda.AwsHelpers;

/**
 * Lambda function which handles Timeout and OutOfMemory error that might occur during CSV import.
 */
@Slf4j
public class RuntimeExceptionHandlerLambda implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  private static final String ERROR_MESSAGE = "Fail to cancel dangling jobs,"
      + " please see the CloudWatch logs for more details";

  /**
   * Lambda function handler.
   */
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    Stopwatch timer = Stopwatch.createStarted();
    initializeHandler();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      EventProcessor eventProcessor = new EventProcessor(getBean(handler,
          RegisterJobSupervisor.class));
      eventProcessor.process(input);
    } catch (Exception e) {
      log.error("Error: ", e);
      // The exception will be recorded under in Lambda Errors metrics
      // which allows it to be handled appropriately if necessary
      throw new IOException(ERROR_MESSAGE);
    }
    log.info("Jobs cancelling took {}ms", timer.stop().elapsed(TimeUnit.MILLISECONDS));
  }

  /**
   * Initialize the Application Context.
   */
  private void initializeHandler() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
  }

  /**
   * Get a Bean instance.
   *
   * @param handler The servlet context container.
   * @param beanClass the Bean Java class.
   * @return a Bean instance of parameterized Class.
   */
  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> beanClass) {
    return WebApplicationContextUtils.getWebApplicationContext(handler.getServletContext())
        .getBean(beanClass);
  }

  /**
   * Lambda Timeout and OutOfMemory event processor.
   */
  @Value
  private static class EventProcessor {

    RegisterJobSupervisor registerJobSupervisor;

    public static final String LAMBDA_EXCEPTION =
        "Your file upload has timed out <a href='https://contact.dvla.gov.uk/caz'>contact us</a> to resolve the issue.";

    /**
     * Process event.
     *
     * @param input The event.
     * @throws Exception If the function is unable to cancel the Job
     */
    public void process(String input) throws IOException {
      Object event;
      try {
        event = convert(input, SNSEvent.class);
      } catch (Exception e) {
        event = convert(input, CloudWatchLogsEvent.class);
      }
      if (event != null) {
        if (event instanceof SNSEvent) {
          processSnsEvent((SNSEvent) event);
        } else {
          processCloudWatchLogsEvent((CloudWatchLogsEvent) event);
        }
      }
    }

    /**
     * Process OutOfMemrory CloudWatch Log Event.
     *
     * @param event A CloudWatch Log Event.
     */
    private void processCloudWatchLogsEvent(CloudWatchLogsEvent event) throws IOException {
      byte[] decodedBytes = Base64.getDecoder().decode(event.getAwsLogs().getData());
      String decodedString = decompressByteArray(decodedBytes);
      CloudWatchDataMessage cloudWatchDataMessage = convert(decodedString,
          CloudWatchDataMessage.class);
      for (LogEvent logEvent : cloudWatchDataMessage.getLogEvents()) {
        String message = logEvent.getMessage();
        String input = message.substring(message.indexOf('{'));
        RegisterCsvFromS3LambdaInput originalInput = convert(input,
            RegisterCsvFromS3LambdaInput.class);
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_EXCEPTION);
      }
    }

    /**
     * Process Lambda Timeout SNS Event.
     *
     * @param event A SNS Event.
     */
    private void processSnsEvent(SNSEvent event) throws IOException {
      for (SNSRecord record : event.getRecords()) {
        RegisterCsvFromS3LambdaInput originalInput = convert(record.getSNS().getMessage(),
            RegisterCsvFromS3LambdaInput.class);
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_EXCEPTION);
      }
    }

    /**
     * Cancel the dangling job.
     *
     * @param jobId The job Id.
     */
    private void cancelJob(int jobId, String reason) {
      registerJobSupervisor.markFailureWithValidationErrors(jobId,
          RegisterJobStatus.ABORTED,
          Collections.singletonList(ValidationError.requestProcessingError(reason)));
    }

    /**
     * Deserialize Json content into Java object.
     *
     * @param input The Json content.
     * @param eventClass A Java class.
     * @return A Java instance of the parameterized Class.
     */
    private <T> T convert(String input, Class<T> eventClass)
        throws IOException {
      ObjectMapper obj = new ObjectMapper();
      obj.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      obj.registerModule(new JodaModule());
      return obj.readValue(input, eventClass);
    }

    /**
     * Decompress GZip data.
     *
     * @param bytes A byte array of data compressed in GZIP format.
     * @return A String represent the decompressed data.
     */
    private String decompressByteArray(byte[] bytes) throws IOException {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
      GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
      InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream,
          StandardCharsets.UTF_8);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        output.append(line);
      }
      return output.toString();
    }
  }
}
