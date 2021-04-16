package uk.gov.caz.vcc.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.context.support.WebApplicationContextUtils;

import uk.gov.caz.auditcleanup.AuditCleanupDataService;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.vcc.Application;
import uk.gov.caz.vcc.service.audit.VehicleEntrantDataCleanupService;

/**
 * AWS Lambda handler implementation for cleansing old audit data from the database layer.
 * 
 */
@Slf4j
public class CleanupOldAuditDataLambdaHandler implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  private VehicleEntrantDataCleanupService dataCleanupService;

  private AuditCleanupDataService auditCleanupDataService;

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    initializeHandlerIfNull();

    dataCleanupService.cleanupData();

    auditCleanupDataService.cleanupOldAuditData();
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
      dataCleanupService = getBean(handler, VehicleEntrantDataCleanupService.class);
      auditCleanupDataService = getBean(handler, AuditCleanupDataService.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> classToInstantiate) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(classToInstantiate);
  }
}
