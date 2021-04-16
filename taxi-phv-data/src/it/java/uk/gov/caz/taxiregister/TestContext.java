package uk.gov.caz.taxiregister;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.ses.SesClient;
import uk.gov.caz.taxiregister.service.SesEmailSender;

@Configuration
public class TestContext {

  @MockBean
  private SesClient sesClient;

  @Bean
  @Primary
  public SesEmailSender stubbedSesEmailSender() {
    return new StubbedSesEmailSender();
  }
}
