package uk.gov.caz.accounts.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.accounts.Application;
import uk.gov.caz.accounts.amazonaws.StreamLambdaHandler.LambdaContainerStats;
import uk.gov.caz.accounts.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.accounts.service.RegisterFromCsvCommand;
import uk.gov.caz.accounts.service.RegisterServiceContext;
import uk.gov.caz.accounts.service.registerjob.RegisterResult;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.correlationid.Constants;

/**
 * Lambda function responsible for the process of parsing CSV files from S3 bucket and adding new
 * vehicles to fleet.
 */
@Slf4j
public class FleetVehiclesRegisterCsvFromS3Lambda implements
    RequestHandler<RegisterCsvFromS3LambdaInput, String> {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private RegisterServiceContext registerServiceContext;

  @Override
  public String handleRequest(RegisterCsvFromS3LambdaInput registerCsvFromS3LambdaInput,
      Context context) {
    if (isWarmerPing(registerCsvFromS3LambdaInput)) {
      return LambdaContainerStats.getStats();
    }
    LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
    Preconditions.checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getS3Bucket()),
        "Invalid input, 's3Bucket' is blank or null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getFileName()),
        "Invalid input, 'fileName' is blank or null");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getCorrelationId()),
            "Invalid input, 'correlationId' is blank or null");

    Stopwatch timer = Stopwatch.createStarted();
    ObjectMapper obj = new ObjectMapper();
    String registerResult = "false";
    initializeHandlerAndService();
    log.info("Handler initialization took {}", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      setCorrelationIdInMdc(registerCsvFromS3LambdaInput.getCorrelationId());
      RegisterResult result = runCsvRegistration(registerCsvFromS3LambdaInput);
      registerResult = String.valueOf(result);
      log.info("Register method took {}", timer.stop().elapsed(TimeUnit.MILLISECONDS));
    } catch (OutOfMemoryError error) {
      try {
        log.info("OutOfMemoryError RegisterCsvFromS3Lambda {}",
            obj.writeValueAsString(registerCsvFromS3LambdaInput));
      } catch (JsonProcessingException e) {
        log.error("JsonProcessingException", e);
      }
    } finally {
      removeCorrelationIdFromMdc();
    }
    return registerResult;
  }

  private void setCorrelationIdInMdc(String correlationId) {
    MDC.put(Constants.X_CORRELATION_ID_HEADER, correlationId);
  }

  private void removeCorrelationIdFromMdc() {
    MDC.remove(Constants.X_CORRELATION_ID_HEADER);
  }

  private boolean isWarmerPing(RegisterCsvFromS3LambdaInput registerCsvFromS3LambdaInput) {
    String action = registerCsvFromS3LambdaInput.getAction();
    if (Strings.isNullOrEmpty(action)) {
      return false;
    }
    return action.equalsIgnoreCase("keep-warm");
  }

  private void initializeHandlerAndService() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
      registerServiceContext = getBean(handler, RegisterServiceContext.class);
    }
  }

  private RegisterResult runCsvRegistration(RegisterCsvFromS3LambdaInput input) {
    return new RegisterFromCsvCommand(
        registerServiceContext,
        input.getRegisterJobId(),
        input.getCorrelationId(),
        input.getS3Bucket(),
        input.getFileName(),
        input.shouldSendEmailsUponSuccessfulChargeCalculation()
    ).register();
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(exampleServiceClass);
  }
}
