package uk.gov.caz.vcc.configuration;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.correlationid.MdcCorrelationIdInjector;
import uk.gov.caz.vcc.repository.NationalTaxiRegisterAsyncRepository;
import uk.gov.caz.vcc.repository.PaymentsRepository;
import uk.gov.caz.vcc.repository.RetrofitModRepository;
import uk.gov.caz.vcc.repository.VehicleRemoteAsyncRepository;
import uk.gov.caz.vcc.service.NoOpPaymentsService;
import uk.gov.caz.vcc.service.PaymentsService;
import uk.gov.caz.vcc.service.RemotePaymentsService;
import uk.gov.caz.vcc.service.VehicleEntrantsBulkPaymentsDataSupplier;
import uk.gov.caz.vcc.service.VehicleEntrantsPaymentsDataSupplier;
import uk.gov.caz.vcc.service.VehicleEntrantsSequentialPaymentsDataSupplier;

/**
 * Configuration to setup retrofit2 services.
 */
@Configuration
@Slf4j
public class AsyncRestConfiguration {

  private static final String SLASH = "/";

  @Value("${dvla-api-endpoint}")
  private String dvlaApiEndpoint;

  @Value("${services.national-taxi-register.root-url}")
  private String ntrApiEndpoint;

  @Value("${services.tariff-service.root-url}")
  private String tariffApiEndpoint;

  @Value("${services.remote-vehicle-data.use-remote-api}")
  private boolean useRemoteApi;

  @Value("${services.mod.root-url}")
  private String modApiEndpoint;

  /**
   * Retrofit2 dvla spring bean.
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit dvlaRetrofit(JacksonConverterFactory jacksonConverterFactory,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return useRemoteApi
        ? new Retrofit.Builder()
            .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(dvlaApiEndpoint))))
            .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
            .addConverterFactory(jacksonConverterFactory).build()
        : new Retrofit.Builder().baseUrl("http://fake/").build();
  }

  /**
   * VehicleRemoteAsyncRepository spring bean.
   *
   * @return {@link VehicleRemoteAsyncRepository}
   */
  @Bean
  public VehicleRemoteAsyncRepository vehicleRepository(Retrofit dvlaRetrofit) {
    return dvlaRetrofit.create(VehicleRemoteAsyncRepository.class);
  }

  /**
   * Retrofit2 ntr spring bean.
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit ntrRetrofit(JacksonConverterFactory jacksonConverterFactory,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(ntrApiEndpoint))))
        .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
        .addConverterFactory(jacksonConverterFactory)
        .build();
  }

  /**
   * Retrofit2 ntr spring bean.
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit modRetrofit(JacksonConverterFactory jacksonConverterFactory,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(modApiEndpoint))))
        .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
        .addConverterFactory(jacksonConverterFactory)
        .build();
  }

  /**
   * NtrRepository spring bean.
   *
   * @return {@link NationalTaxiRegisterAsyncRepository}
   */
  @Bean
  public NationalTaxiRegisterAsyncRepository ntrRepository(Retrofit ntrRetrofit) {
    return ntrRetrofit.create(NationalTaxiRegisterAsyncRepository.class);
  }

  @Bean
  public RetrofitModRepository retrofitModRepository(Retrofit modRetrofit) {
    return modRetrofit.create(RetrofitModRepository.class);
  }

  /**
   * Creates a no-op version of {@link PaymentsService} when integration with PSR is disabled,
   * an instance of {@link RemotePaymentsService} otherwise.
   */
  @Bean
  public PaymentsService paymentsService(
      @Value("${services.payments.enabled:false}") boolean paymentsEnabled,
      @Value("${services.payments.root-url}") String paymentsApiEndpoint,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds,
      AsyncRestService asyncRestService,
      JacksonConverterFactory jacksonConverterFactory) {
    log.info("Payments service configuration: root url '{}', enabled '{}'", paymentsApiEndpoint,
        paymentsEnabled);
    if (paymentsEnabled) {
      PaymentsRepository paymentsRepository = paymentsRepository(paymentsApiEndpoint,
          readTimeoutSeconds, connectTimeoutSeconds, jacksonConverterFactory);
      return new RemotePaymentsService(paymentsRepository, asyncRestService);
    }
    log.info("Integration with PSR is DISABLED, creating a no-op payments service");
    return new NoOpPaymentsService();
  }

  /**
   * Creates an instance of {@link VehicleEntrantsPaymentsDataSupplier}.
   */
  @Bean
  public VehicleEntrantsPaymentsDataSupplier vehicleEntrantsPaymentsDataSupplier(
      @Value("${services.payments.use-bulk-vehicle-entrants-endpoint:false}")
          boolean bulkOperationsEnabled, PaymentsService paymentsService) {
    if (bulkOperationsEnabled) {
      log.info("Bulk operations ENABLED for PSR");
      return new VehicleEntrantsBulkPaymentsDataSupplier(paymentsService);
    }
    log.info("Bulk operations DISABLED for PSR");
    return new VehicleEntrantsSequentialPaymentsDataSupplier(paymentsService);
  }

  /**
   * PaymentsRepository retrofit client.
   *
   * @return {@link PaymentsRepository}
   */
  private PaymentsRepository paymentsRepository(String paymentsApiEndpoint,
      int readTimeoutSeconds, int connectTimeoutSeconds,
      JacksonConverterFactory jacksonConverterFactory) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(paymentsApiEndpoint))))
        .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
        .addConverterFactory(jacksonConverterFactory)
        .build()
        .create(PaymentsRepository.class);
  }

  /**
   * Retrofit2 JacksonConverterFactory spring bean.
   *
   * @return {@link JacksonConverterFactory}
   */
  @Bean
  public JacksonConverterFactory jacksonConverterFactory(ObjectMapper objectMapper) {
    return JacksonConverterFactory.create(objectMapper);
  }

  /**
   * Helper method to format url.
   *
   * @param url to format
   * @return url ended with slash
   */
  private static String formatUrl(String url) {
    return url.endsWith(SLASH) ? url : url + SLASH;
  }


  /**
   * Retrofit2 tariff spring bean.
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit tariffRetrofit(JacksonConverterFactory jacksonConverterFactory,
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(tariffApiEndpoint))))
        .client(buildHttpClient(readTimeoutSeconds, connectTimeoutSeconds))
        .addConverterFactory(jacksonConverterFactory)
        .build();
  }

  /**
   * Helper method to build HttpClient for the request.
   *
   * @return OkHttpClient with all needed attributes.
   */
  private OkHttpClient buildHttpClient(int readTimeoutInSeconds, int connectTimeoutInSeconds) {
    return new Builder()
        .readTimeout(readTimeoutInSeconds, TimeUnit.SECONDS)
        .connectTimeout(connectTimeoutInSeconds, TimeUnit.SECONDS)
        .addInterceptor(chain -> {
          Request original = chain.request();
          Request withCorrelationIdHeader = original.newBuilder()
              .header(Constants.X_CORRELATION_ID_HEADER, getOrGenerateCorrelationId(
                  MdcCorrelationIdInjector.getCurrentValue()))
              .build();
          return chain.proceed(withCorrelationIdHeader);
        })
        .build();
  }

  /**
   * Helper method to get current or generate new CorrelationId.
   *
   * @param correlationIdFromRequest correlation id from the request (can be null if the logic
   *     is invoked not in the HTTP request context)
   * @return correlationID as a String
   */
  private String getOrGenerateCorrelationId(String correlationIdFromRequest) {
    return correlationIdFromRequest == null
        ? UUID.randomUUID().toString()
        : correlationIdFromRequest;
  }
}

