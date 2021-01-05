package uk.gov.caz.accounts.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
@Profile("!integration-tests")
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfiguration {

  public static final String TAG_REGISTER_CSV_FROM_S3_CONTROLLER = "RegisterCsvFromS3Controller";

  /**
   * Creates a swagger configuration.
   */
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage("uk.gov.caz.accounts.controller"))
        .build();
  }
}
