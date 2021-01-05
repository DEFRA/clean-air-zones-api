package uk.gov.caz.accounts.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.accounts.Application;
import uk.gov.caz.accounts.dto.ChargeCalculationLambdaInput;
import uk.gov.caz.accounts.service.chargecalculation.ChargeCalculationJobSupervisor;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.correlationid.Constants;

/**
 * Lambda function which calculates charges and populates cache.
 */
@Slf4j
public class ChargeCalculationLambdaHandler implements
    RequestHandler<ChargeCalculationLambdaInput, String> {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  @Override
  public String handleRequest(ChargeCalculationLambdaInput request, Context context) {
    Stopwatch timer = Stopwatch.createStarted();
    request.validate();
    initializeHandlerIfNull();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      setCorrelationIdInMdc(request.getCorrelationId().toString());
      ChargeCalculationJobSupervisor chargeCalculationJobSupervisor = getBean(handler,
          ChargeCalculationJobSupervisor.class);
      chargeCalculationJobSupervisor.populateChargeCalculationCache(request.getAccountId(),
          request.getJobId(), request.getInvocationNumber(), request.getCorrelationId(),
          request.shouldSendEmailsUponSuccessfulJobCompletion());
      return "OK";
    } finally {
      log.info("Fleet Chargeability population method took {}ms",
          timer.stop().elapsed(TimeUnit.MILLISECONDS));
      removeCorrelationIdFromMdc();
    }
  }

  private void setCorrelationIdInMdc(String correlationId) {
    MDC.put(Constants.X_CORRELATION_ID_HEADER, correlationId);
  }

  private void removeCorrelationIdFromMdc() {
    MDC.remove(Constants.X_CORRELATION_ID_HEADER);
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(exampleServiceClass);
  }
}