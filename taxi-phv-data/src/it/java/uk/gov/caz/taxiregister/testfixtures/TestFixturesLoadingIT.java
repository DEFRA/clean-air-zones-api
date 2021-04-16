package uk.gov.caz.taxiregister.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.taxiregister.annotation.MockedMvcIntegrationTest;

@MockedMvcIntegrationTest
@Sql(scripts = {
    "classpath:data/sql/taxi-phv-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
public class TestFixturesLoadingIT {

  private static final String PATH = "/v1/load-test-data";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void testLoadingTestFixtures() throws Exception {
    executeRequest()
        .andExpect(status().isNoContent());

    verifyLicencesHaveBeenLoaded();
  }

  private void verifyLicencesHaveBeenLoaded() {
    int licencesCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "t_md_taxi_phv");
    assertThat(licencesCount).isEqualTo(2);
  }

  private ResultActions executeRequest() throws Exception {
    return mockMvc.perform(post(PATH)
        .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID()));
  }
}
