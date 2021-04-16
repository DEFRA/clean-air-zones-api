package uk.gov.caz.vcc.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.vcc.Application;
import uk.gov.caz.vcc.service.ReportingDataService;

/**
 * AWS Lambda handler implementation for parsing SQS messages 
 * that hold contents to be written to the reporting schema.
 * 
 */
public class ReportingDataHandler implements RequestHandler<SQSEvent, Void> {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler; 

  @Override
  public Void handleRequest(SQSEvent event, Context context) {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
    for (SQSMessage msg : event.getRecords()) {    
      String messageBody = msg.getBody();
      String messageId = msg.getMessageId();
      ReportingDataService reportingDataService = getBean(handler, ReportingDataService.class);
      reportingDataService.process(messageBody, messageId);
    }
    return null;
  }

  /**
   * Private helper for instantiating a DataReportingServer instance.
   */
  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> classToInstantiate) {
    return WebApplicationContextUtils.getWebApplicationContext(handler.getServletContext())
        .getBean(classToInstantiate);
  }
}
