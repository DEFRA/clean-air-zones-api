package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.util.BulkCheckerTestUtility;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
@TestPropertySource(properties = "services.mod.use-bulk-search-endpoint=true")
public class MilitaryVehicleServiceBulkSearchTestIT extends MockServerTestIT {

  @Autowired
  MilitaryVehicleService militaryVehicleService;

  @Autowired
  BulkCheckerTestUtility testUtility;

  @Test
  public void shouldReturnModVehicleVrns() {
    // given
    String militaryVrn = "CAS312";
    Set<String> vrns = ImmutableSet.of(militaryVrn, "CAS313");
    setupTestData();

    // when
    Set<String> result = militaryVehicleService.filterMilitaryVrnsFromList(vrns);

    // then
    assertThat(result).containsOnly(militaryVrn);
  }

  private void setupTestData() {
    mockModForVrns();
  }
}