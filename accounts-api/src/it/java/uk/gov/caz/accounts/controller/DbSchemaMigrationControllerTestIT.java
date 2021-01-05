package uk.gov.caz.accounts.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.util.LiquibaseWrapper;

@MockedMvcIntegrationTest
public class DbSchemaMigrationControllerTestIT {

  @MockBean
  private LiquibaseWrapper liquibaseWrapper;

  @Autowired
  private MockMvc mockMvc;

  private HttpHeaders headers;

  @BeforeEach
  public void setup() {
    this.headers = new HttpHeaders();
    this.headers.add("X-Correlation-ID", "5eb7577f-53ab-4b5c-ab9e-10388b9b869b");
  }

  @Test
  public void shouldReturn200OnSuccess() throws Exception {
    mockMvc.perform(post(DbSchemaMigrationControllerApi.PATH).headers(headers))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  public void shouldReturn500WhenCannotConnectToDatabase() throws Exception {
    doThrow(SQLException.class).when(liquibaseWrapper).update();

    mockMvc.perform(post(DbSchemaMigrationControllerApi.PATH).headers(headers))
        .andExpect(status().is5xxServerError());
  }

  @Test
  public void shouldReturn500WhenCannotPerformDbUpgrade() throws Exception {
    doThrow(LiquibaseException.class).when(liquibaseWrapper).update();

    mockMvc.perform(post(DbSchemaMigrationControllerApi.PATH).headers(headers))
        .andExpect(status().is5xxServerError());
  }
}
