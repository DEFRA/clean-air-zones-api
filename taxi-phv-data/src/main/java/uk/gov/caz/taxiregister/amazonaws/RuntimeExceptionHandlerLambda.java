package uk.gov.caz.taxiregister.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.taxiregister.Application;
import uk.gov.caz.taxiregister.dto.CloudWatchDataMessage;
import uk.gov.caz.taxiregister.dto.CloudWatchDataMessage.LogEvent;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.taxiregister.dto.RegisterFromRestApiInput;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterServicesContext;

@Slf4j
/**
 * Lambda function which handles Timeout and OutOfMemory error
 * that might occur during Taxi/PHV csv import 
 */
public class RuntimeExceptionHandlerLambda implements RequestStreamHandler  {
  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private RegisterServicesContext registerServicesContext;
  
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
    initializeHandlerAndService();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      EventProcessor eventProcessor = new EventProcessor(registerServicesContext);
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
  private void initializeHandlerAndService() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
      registerServicesContext = getBean(handler, RegisterServicesContext.class);
    }
  }
  
  /**
   * Get a Bean instance.
   * @param handler The servlet context container.
   * @param beanClass the Bean Java class.
   * @return a Bean instance of parameterized Class.
   */
  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> beanClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(beanClass);
  }
  
  /**
   * Lambda Timeout and OutOfMemory event processor.
   */
  public static class EventProcessor {
    private RegisterJobSupervisor registerJobSupervisor;
    private SqsClient sqsClient;
    private RegisterServicesContext registerServicesContext;
    
    public static final String LAMBDA_TIMEOUT_EXCEPTION = "Lambda timeout exception. "
        + "Please contact administrator to get assisstance.";
    public static final String LAMBDA_OUTOFMEMORY_EXCEPTION = "OutOfMemory exception. "
        + "Please contact administrator to get assisstance.";
    
    /**
     * Creates an {@link RuntimeExceptionHandlerLambda.EventProcessor}.
     * @param registerServicesContext a RegisterServicesContext instance.
     */
    public EventProcessor(RegisterServicesContext registerServicesContext) {
      this.registerJobSupervisor = registerServicesContext.getRegisterJobSupervisor();
      this.sqsClient = registerServicesContext.getSqsClient();
      this.registerServicesContext = registerServicesContext;
    }
    
    /**
     * Process event.
     * @param input The event.
     * @throws Exception If the function is unable to cancel the Job
     */
    public void process(String input) throws IOException {
      Object event;
      try {
        event = convert(input,SNSEvent.class);
      } catch (Exception e) {
        try {
          event = convert(input,CloudWatchLogsEvent.class);
        } catch (Exception ex) {
          event = convert(input,SQSEvent.class);
        }
      }
      if (event != null) {
        if (event instanceof SNSEvent) {
          processSnsEvent((SNSEvent)event);
        } else if (event instanceof CloudWatchLogsEvent) {
          processCloudWatchLogsEvent((CloudWatchLogsEvent)event);
        } else if (event instanceof SQSEvent) {
          processSqsEvent((SQSEvent)event);
        }
      }
    }
    
    /**
     * Process OutOfMemrory CloudWatch Log Event.
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
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_OUTOFMEMORY_EXCEPTION);
      }
    }

    /**
     * Process Lambda Timeout SNS Event.
     * @param event A SNS Event.
     */
    private void processSnsEvent(SNSEvent event) throws IOException {
      
      for (SNSRecord record : event.getRecords()) {
        RegisterCsvFromS3LambdaInput originalInput = convert(record.getSNS().getMessage(),
            RegisterCsvFromS3LambdaInput.class);
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_TIMEOUT_EXCEPTION);
      }
    }
    
    /**
     * Process Lambda Timeout SQS Event.
     * @param event A SQS Event.
     */
    private void processSqsEvent(SQSEvent event) throws IOException {
      for (SQSMessage record : event.getRecords()) {
        RegisterFromRestApiInput registerFromRestApiInput = convert(record.getBody(),
            RegisterFromRestApiInput.class);
        Optional<RegisterJob> registerJob = 
            queryJobStatus(registerFromRestApiInput.getRegisterJobId());
        if (registerJob.isPresent()) {
          RegisterJob theJob = registerJob.get();
          if (theJob.getStatus() == RegisterJobStatus.RUNNING) {
            cancelJob(theJob.getId(), EventProcessor.LAMBDA_TIMEOUT_EXCEPTION);
          }
        }
        String messageReceiptHandler = record.getReceiptHandle();
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
            .receiptHandle(messageReceiptHandler)
            .queueUrl(registerServicesContext.getJobCleanupRequestQueueUrl())
            .build());
      }
    }
    
    /**
     * Cancel the dangling job.
     * @param jobId The job Id.
     */
    private void cancelJob(int jobId, String reason) {
      registerJobSupervisor.markFailureWithValidationErrors(jobId,
          RegisterJobStatus.ABORTED,
          Arrays.asList(ValidationError.requestProcessingError(reason)));
    }
    
    /**
     * Query job status.
     * @param registerJobId The job Id.
     */
    private Optional<RegisterJob> queryJobStatus(int registerJobId) {
      return registerJobSupervisor.findJobById(registerJobId);
    }
    
    
    /**
     * Deserialize Json content into Java object.
     * @param input The Json content.
     * @param eventClass A Java class.
     * @return A Java instance of the parameterized Class.
     */
    private <T> T convert(String input, Class<T> eventClass) throws IOException {
      ObjectMapper obj = new ObjectMapper();
      obj.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      obj.registerModule(new JodaModule());
      return obj.readValue(input, eventClass);
    }
    
    /**
     * Decompress GZip data.
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
