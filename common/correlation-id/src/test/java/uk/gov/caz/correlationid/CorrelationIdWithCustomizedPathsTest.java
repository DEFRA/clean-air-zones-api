package uk.gov.caz.correlationid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@ContextConfiguration(classes = {Configuration.class, TestController.class})
@WebMvcTest
@ActiveProfiles(profiles = "paths")
class CorrelationIdWithCustomizedPathsTest {

  @Autowired
  private MockMvc mockMvc;

  /**
   * Calls for /v1/** which is NOT configured in application-paths.yml config file as included for
   * X-Correlation-ID capture and injection.
   */
  @Nested
  class V1Path {

    @Nested
    class ShouldGet200 {

      @Test
      void ifXCorrelationIdHeaderIsPresent()
          throws Exception {
        mockMvc.perform(get("/v1/test-with-response-body")
            .header(X_CORRELATION_ID_HEADER, UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));
      }

      @Test
      void ifXCorrelationIdHeaderIsNotPresent()
          throws Exception {
        mockMvc.perform(get("/v1/test-with-response-body"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));
      }
    }
  }

  /**
   * Calls for /customized/** which is configured in application-paths.yml config file as included
   * for X-Correlation-ID capture and injection.
   */
  @Nested
  class CustomizedPath {

    @Nested
    class ShouldGet200 {

      @Test
      void ifXCorrelationIdHeaderIsPresent()
          throws Exception {
        mockMvc.perform(get("/customized/test")
            .header(X_CORRELATION_ID_HEADER, UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(header().exists(X_CORRELATION_ID_HEADER));
      }
    }

    @Nested
    class ShouldGet400 {

      @Test
      void ifXCorrelationIdHeaderIsNotPresent()
          throws Exception {
        mockMvc.perform(get("/customized/test"))
            .andExpect(status().isBadRequest());
      }
    }
  }
}
