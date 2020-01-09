package uk.gov.caz.vcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.gov.caz.vcc.configuration.RequestMappingConfiguration;
import uk.gov.caz.vcc.configuration.SwaggerConfiguration;

@Import({RequestMappingConfiguration.class, SwaggerConfiguration.class})
@EntityScan("uk.gov.caz.vcc.domain")
@EnableJpaRepositories("uk.gov.caz.vcc.repository")
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
