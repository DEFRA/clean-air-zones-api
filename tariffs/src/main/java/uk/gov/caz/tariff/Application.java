package uk.gov.caz.tariff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import uk.gov.caz.tariff.configuration.RequestMappingConfiguration;
import uk.gov.caz.tariff.configuration.SwaggerConfiguration;
import uk.gov.caz.tariff.controller.CleanAirZonesController;
import uk.gov.caz.tariff.service.CleanAirZonesRepository;
import uk.gov.caz.tariff.service.TariffRepository;

@Import({
    RequestMappingConfiguration.class,
    SwaggerConfiguration.class,
    CleanAirZonesController.class,
    TariffRepository.class,
    CleanAirZonesRepository.class
})
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}