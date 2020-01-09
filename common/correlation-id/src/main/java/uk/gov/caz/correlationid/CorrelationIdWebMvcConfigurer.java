package uk.gov.caz.correlationid;

import lombok.AllArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AllArgsConstructor
public class CorrelationIdWebMvcConfigurer implements WebMvcConfigurer {

  private final CorrelationIdProperties correlationIdProperties;
  private final MdcAdapter mdcAdapter;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new MdcCorrelationIdInjector(mdcAdapter))
        .addPathPatterns(correlationIdProperties.getIncludedPathPatterns());
  }
}
