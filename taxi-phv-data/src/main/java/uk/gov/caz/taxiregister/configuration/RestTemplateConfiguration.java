package uk.gov.caz.taxiregister.configuration;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.correlationid.MdcCorrelationIdInjector.getCurrentValue;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * Spring Configuration class for {@link RestTemplate} operations.
 */
@Configuration
public class RestTemplateConfiguration {

  /**
   * Creates and intialized {@link RestTemplateBuilder} used to get {@link
   * org.springframework.web.client.RestTemplate} instances.
   *
   * @param readTimeoutSeconds the timeout on waiting to read data.
   * @param connectTimeoutSeconds timeout for making the initial connection.
   * @return A configured RestTemplateBuilder.
   */
  @Bean
  public RestTemplateBuilder commonRestTemplateBuilder(
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return new RestTemplateBuilder()
        .interceptors(correlationIdAppendingInterceptor())
        .setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
  }

  /**
   * Creates {@link ClientHttpRequestInterceptor} that adds X-Correlation-ID header that came from
   * original API request. Header is then passed along the call path.
   *
   * @return {@link ClientHttpRequestInterceptor} with X-Correlation-ID header.
   */
  private ClientHttpRequestInterceptor correlationIdAppendingInterceptor() {
    return (request, body, execution) -> {
      request.getHeaders().add(X_CORRELATION_ID_HEADER, getCurrentValue());
      return execution.execute(request, body);
    };
  }
}
