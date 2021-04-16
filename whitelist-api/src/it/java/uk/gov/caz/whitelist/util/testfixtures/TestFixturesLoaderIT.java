package uk.gov.caz.whitelist.util.testfixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.whitelist.annotation.MockedMvcIntegrationTest;

@MockedMvcIntegrationTest
@Sql(scripts = {
    "classpath:data/sql/whitelist-vehicles-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class TestFixturesLoaderIT {

  private static final String PATH = "/v1/load-test-data";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void shouldLoadTestFixtures() throws Exception {
    executeRequest()
        .andExpect(status().isNoContent());

    verifyVehiclesThatHaveBeenLoaded();
  }

  private void verifyVehiclesThatHaveBeenLoaded() {
    int whitelistedVehicles = JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_whitelist_vehicles.t_whitelist_vehicles");
    assertThat(whitelistedVehicles).isEqualTo(4);
  }

  private ResultActions executeRequest() throws Exception {
    return mockMvc.perform(post(PATH)
        .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID()));
  }
}