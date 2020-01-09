package uk.gov.caz.vcc.cucumber;

import io.cucumber.java.Before;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.caz.vcc.Application;

@SpringBootTest
@ContextConfiguration(classes = Application.class, loader = SpringBootContextLoader.class)
@ActiveProfiles("integration-tests")
public class CucumberSpringContextConfiguration {
  private static final Logger log = LoggerFactory.getLogger(CucumberSpringContextConfiguration.class);

  @Before
  public void setup() {
    log.info("-------------- Spring Context Initialized For Executing Cucumber Tests --------------");
  }
}