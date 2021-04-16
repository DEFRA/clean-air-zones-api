package uk.gov.caz.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.repository.ModDataProvider;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty"}, features = "src/it/resources/cucumber")
public class RunCucumberIT {

  @Configuration
  @ComponentScan("uk.gov.caz.vcc")
  @ComponentScan("uk.gov.caz.cucumber")
  static class TestContext {
    @Bean
    @Primary
    public ModDataProvider stubbedModDataProvider() {
      return new ModDataProvider() {
        @Override
        public Boolean existsByVrnIgnoreCase(String vrn) {
          return false;
        }

        @Override
        public MilitaryVehicle findByVrnIgnoreCase(String vrn) {
          return new MilitaryVehicle();
        }

        @Override
        public Map<String, Boolean> existByVrns(Set<String> vrns) {
          return vrns.stream().collect(Collectors.toMap(Function.identity(), vrn -> true));
        }
      };
    }
  }
}