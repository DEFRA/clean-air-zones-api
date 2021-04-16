package uk.gov.caz.vcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.service.MilitaryVehicleService.MilitaryVehicleQueryResponse;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
public class MilitaryVehicleServiceTestIT extends MockServerTestIT {

  @Autowired
  MilitaryVehicleService militaryVehicleService;

  final String VRN = "CAS312";

  @BeforeEach
  public void setup() {
    setupTestData(VRN);
  }

  @Test
  public void shouldReturnModVehicleVrns() {
    List<String> vrns = Collections.singletonList(VRN);
    MilitaryVehicleQueryResponse response = militaryVehicleService
        .extractMilitaryVehiclesOutOf(vrns, null, 60);
    assertEquals(1, response.getMilitaryVehicleVrns().size());
    assertEquals(0, response.getUnProcessedVrns().size());
  }

  private void setupTestData(String vrn) {
    mockModForVrn(vrn);
  }
}