package uk.gov.caz.vcc.configuration;

import static java.util.Objects.requireNonNull;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import uk.gov.caz.vcc.repository.NtrRepository;
import uk.gov.caz.vcc.repository.VehicleRepository;

/**
 * Configuration to setup retrofit2 services
 */
@Configuration
public class AsyncRestConfiguration {

  private static final String SLASH = "/";

  @Value("${dvla-api-endpoint}")
  private String dvlaApiEndpoint;

  @Value("${services.national-taxi-register.root-url}")
  private String ntrApiEndpoint;

  @Value("${services.remote-vehicle-data.use-remote-api}")
  private boolean useRemoteApi;

  /**
   * Retrofit2 dvla spring bean
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit dvlaRetrofit() {
    return useRemoteApi ? new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(dvlaApiEndpoint))))
        .addConverterFactory(jacksonConverterFactory())
        .build() : new Retrofit.Builder()
        .baseUrl("http://fake/")
        .build();
  }

  /**
   * VehicleRepository spring bean
   *
   * @return {@link VehicleRepository}
   */
  @Bean
  public VehicleRepository vehicleRepository(Retrofit dvlaRetrofit) {
    return dvlaRetrofit.create(VehicleRepository.class);
  }

  /**
   * Retrofit2 ntr spring bean
   *
   * @return {@link Retrofit}
   */
  @Bean
  public Retrofit ntrRetrofit(JacksonConverterFactory jacksonConverterFactory) {
    return new Retrofit.Builder()
        .baseUrl(requireNonNull(HttpUrl.parse(formatUrl(ntrApiEndpoint))))
        .addConverterFactory(jacksonConverterFactory)
        .build();
  }

  /**
   * NtrRepository spring bean
   *
   * @return {@link NtrRepository}
   */
  @Bean
  public NtrRepository ntrRepository(Retrofit ntrRetrofit) {
    return ntrRetrofit.create(NtrRepository.class);
  }

  /**
   * Retrofit2 JacksonConverterFactory spring bean
   *
   * @return {@link JacksonConverterFactory}
   */
  @Bean
  public JacksonConverterFactory jacksonConverterFactory() {
    return JacksonConverterFactory.create();
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
}

