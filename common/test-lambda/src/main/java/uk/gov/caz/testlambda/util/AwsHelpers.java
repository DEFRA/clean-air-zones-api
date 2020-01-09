package uk.gov.caz.testlambda.util;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.google.common.base.Splitter;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class AwsHelpers {

  private static final Splitter SPLITTER = Splitter.on(',')
      .trimResults()
      .omitEmptyStrings();

  /**
   * Determines if code is running locally using AWS SAM Local tool.
   *
   * @return true if code is running in Lambda environment simulated locally by AWS SAM Local tool
   *     (docker).
   */
  public static boolean areWeRunningLocallyUsingSam() {
    return System.getenv("AWS_SAM_LOCAL") != null;
  }

  /**
   * Returns value of AWS_ACCESS_KEY_ID environment variable (if set).
   *
   * @return value of AWS_ACCESS_KEY_ID environment variable if set or null otherwise.
   */
  public static String getAwsAccessKeyFromEnvVar() {
    return System.getenv("AWS_ACCESS_KEY_ID");
  }

  /**
   * Returns value of AWS_REGION environment variable (if set).
   *
   * @return value of AWS_REGION environment variable if set or null otherwise.
   */
  public static String getAwsRegionFromEnvVar() {
    return System.getenv("AWS_REGION");
  }

  /**
   * Returns value of AWS_PROFILE environment variable (if set).
   *
   * @return value of AWS_PROFILE environment variable if set or null otherwise.
   */
  public static String getAwsProfileFromEnvVar() {
    return System.getenv("AWS_PROFILE");
  }

  /**
   * This method creates {@link SpringBootLambdaContainerHandler} that connects Lambda with
   * Spring-Boot application.
   * <p>
   * Handler instance then takes Lambda invocation context and transparently passes it into proper
   * Spring-Boot controller endpoint or to any custom service bean method.
   *
   * This method takes system environment variable "SPRING_PROFILES_ACTIVE" and activates selected
   * Spring profiles.
   * </p>
   *
   * @param applicationClass a main Application class used to initialize Spring Boot
   * @return {@link SpringBootLambdaContainerHandler} instance that connects Lambda with Spring-Boot
   *     application.
   * @throws RuntimeException in case of failure to initialize Spring-Boot context.
   */
  public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>
      initSpringBootHandler(Class<?> applicationClass) {
    SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    try {
      String listOfActiveSpringProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
      if (listOfActiveSpringProfiles != null) {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(applicationClass,
            splitToArray(listOfActiveSpringProfiles));
      } else {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(applicationClass);
      }
      return handler;
    } catch (ContainerInitializationException e) {
      // if we fail here. We re-throw the exception to force another cold start
      throw new IllegalStateException("Could not initialize Spring Boot application", e);
    }
  }

  public static String[] splitToArray(String activeProfiles) {
    return SPLITTER.splitToList(activeProfiles).toArray(new String[0]);
  }
}
