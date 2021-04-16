package uk.gov.caz.whitelist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import uk.gov.caz.db.exporter.destination.s3.AwsS3DestinationConfiguration;
import uk.gov.caz.whitelist.configuration.AwsConfiguration;
import uk.gov.caz.whitelist.configuration.DbExporterConfiguration;
import uk.gov.caz.whitelist.configuration.RequestMappingConfiguration;
import uk.gov.caz.whitelist.configuration.SwaggerConfiguration;

@Import({
    RequestMappingConfiguration.class,
    SwaggerConfiguration.class,
    AwsConfiguration.class,
    AwsS3DestinationConfiguration.class,
    DbExporterConfiguration.class})
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}