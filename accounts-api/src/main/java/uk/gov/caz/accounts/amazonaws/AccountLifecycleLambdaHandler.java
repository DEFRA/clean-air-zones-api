package uk.gov.caz.accounts.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.accounts.Application;
import uk.gov.caz.accounts.service.ProcessInactiveUsersService;
import uk.gov.caz.awslambda.AwsHelpers;

/**
 * Lambda function that handles lifecycle events relevant to Accounts.
 */
@Slf4j
public class AccountLifecycleLambdaHandler implements RequestStreamHandler {

  private static final String ACTION_TO_PROCESS_INACTIVE_ACCOUNTS = "process-inactive-accounts";
  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  /**
   * Lambda function handler.
   */
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    Stopwatch timer = Stopwatch.createStarted();
    initializeHandler();
    log.info("AccountLifecycleLambdaHandler initialization took {}ms",
        timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      val action = parseAsAction(input);
      if (action.action.equalsIgnoreCase(ACTION_TO_PROCESS_INACTIVE_ACCOUNTS)) {
        log.info("Executing action '{}'", ACTION_TO_PROCESS_INACTIVE_ACCOUNTS);
        val processInactiveUsersService = getBean(handler, ProcessInactiveUsersService.class);
        processInactiveUsersService.execute();
      } else {
        log.info("Unsupported operation for input: {}", input);
      }
    } catch (Exception e) {
      log.error("Error: ", e);
    }
    log.info("AccountLifecycleLambdaHandler took {}ms",
        timer.stop().elapsed(TimeUnit.MILLISECONDS));
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
   * Parses incoming input as {@link Action} instance.
   */
  private Action parseAsAction(String input) throws JsonProcessingException {
    val objectMapper = getBean(handler, ObjectMapper.class);
    val action = objectMapper.readValue(input, Action.class);
    return action;
  }

  /**
   * Class representing incoming JSON input with action.
   */
  @Value
  private static class Action {

    /**
     * Action to execute.
     */
    private String action;
  }
}
