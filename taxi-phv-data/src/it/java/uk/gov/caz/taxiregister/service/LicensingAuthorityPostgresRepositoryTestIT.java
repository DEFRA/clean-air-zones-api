package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/licensing-authority-data.sql",
    "classpath:data/sql/taxi-phv-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class LicensingAuthorityPostgresRepositoryTestIT {

  @Autowired
  private LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  @Test
  public void shouldReturnAuthorityNames() {
    // when
    List<String> authorityNames = licensingAuthorityPostgresRepository
        .findAuthorityNamesByVrm("BD51SMR");

    // then
    assertThat(authorityNames).hasSize(3);
    assertThat(authorityNames).contains("la-1", "la-3", "la-4");
  }

  @Test
  public void shouldReturnEmptyList() {
    // when
    List<String> authorityNames = licensingAuthorityPostgresRepository
        .findAuthorityNamesByVrm("DD51SMR");

    // then
    assertThat(authorityNames).hasSize(0);
  }
}
