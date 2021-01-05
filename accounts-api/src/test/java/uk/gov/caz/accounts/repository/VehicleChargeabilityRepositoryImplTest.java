package uk.gov.caz.accounts.repository;

import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class VehicleChargeabilityRepositoryImplTest {

  @Test
  public void deleteForEmptySetDoesNothing() {
    // given
    JdbcTemplate mockedJdbcTemplate = mock(JdbcTemplate.class);
    VehicleChargeabilityRepositoryImpl repository = new VehicleChargeabilityRepositoryImpl(
        mockedJdbcTemplate);

    // when
    repository.deleteFromVehicleChargeability(emptySet());

    // then
    verifyNoInteractions(mockedJdbcTemplate);
  }
}