package uk.gov.caz.vcc.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.vcc.Application;
import uk.gov.caz.vcc.service.BulkCheckerService;

/**
 *  AWS Lambda handler implementation for responding to CSV bulk check S3 deposits.
 *
 */
public class BulkCheckerHandler implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }

    S3Event event = objectMapper.readValue(input, S3Event.class);
    S3EventNotificationRecord record = event.getRecords().get(0);
    String srcBucket = record.getS3().getBucket().getName();
    String srcKey = record.getS3().getObject().getUrlDecodedKey();
    BulkCheckerService bulkCheckerService = getBean(handler, BulkCheckerService.class);
    bulkCheckerService.process(srcBucket, srcKey, context.getRemainingTimeInMillis() / 1000);

    try (OutputStreamWriter ow = new OutputStreamWriter(output)) {
      ow.write(String.format("File %s has been put to bucket %s", srcKey, srcBucket));
    }
  }

  /**
   * Private helper for instantiating a BulkCheckerService instance.
   */
  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> classToInstantiate) {
    return WebApplicationContextUtils.getWebApplicationContext(handler.getServletContext())
        .getBean(classToInstantiate);
  }
}
