package uk.gov.caz.taxiregister.configuration;

import com.fasterxml.classmate.TypeResolver;
import java.util.Collections;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.caz.taxiregister.configuration.properties.SwaggerApiInfo;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.tasks.AnyTaskEnabled;

@EnableSwagger2
@Configuration
@Profile("!integration-tests")
@Import({BeanValidatorPluginsConfiguration.class, SwaggerApiInfo.class})
@ConditionalOnMissingBean(value = AnyTaskEnabled.class)
public class SwaggerConfiguration {

  public static final String TAG_REGISTER_CONTROLLER = "RegisterController";
  public static final String TAG_REGISTER_CSV_FROM_S3_CONTROLLER = "RegisterCsvFromS3Controller";

  /**
   * Creates a swagger configuration.
   */
  @Bean
  public Docket api(TypeResolver typeResolver, SwaggerApiInfo swaggerApiInfo) {
    return new Docket(DocumentationType.SWAGGER_2)
        .useDefaultResponseMessages(false)
        .select()
        .apis(RequestHandlerSelectors.basePackage("uk.gov.caz.taxiregister.controller"))
        .build()
        .apiInfo(new ApiInfo(
            swaggerApiInfo.getTitle(),
            swaggerApiInfo.getDescription(),
            swaggerApiInfo.getVersion(),
            null,
            null,
            null,
            null,
            Collections.emptyList()
        ))
        .pathMapping("/")
        .tags(new Tag(TAG_REGISTER_CONTROLLER, "Register Controller"))
        .tags(new Tag(TAG_REGISTER_CSV_FROM_S3_CONTROLLER, "Register CSV from S3 Controller"))
        .additionalModels(
            typeResolver.resolve(VehicleDto.class),
            typeResolver.resolve(Vehicles.class),
            typeResolver.resolve(
                StartRegisterCsvFromS3JobCommand.class),
            typeResolver.resolve(RegisterJobStatus.class),
            typeResolver.resolve(StatusOfRegisterCsvFromS3JobQueryResult.class)
        );
  }
}
