package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.repository.ModDataProvider;
import uk.gov.caz.vcc.service.MilitaryVehicleService;
import uk.gov.caz.vcc.service.MilitaryVehicleService.MilitaryVehicleQueryResponse;

@ExtendWith(MockitoExtension.class)
public class MilitaryVehicleServiceTest {
  @Mock
  ModDataProvider mockModDataProvider;

  final String TEST_VRN = "InvalidSQL";

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mockModDataProvider.existsByVrnIgnoreCase(TEST_VRN)).thenThrow(RuntimeException.class);
  }
  
  @Test
  public void shouldIncludeVrnInUnprocessedList() {
    MilitaryVehicleService service = new MilitaryVehicleService(mockModDataProvider, false);
    MilitaryVehicleQueryResponse response = service.extractMilitaryVehiclesOutOf(Collections.singletonList(TEST_VRN), null, 60);
    assertEquals(1, response.getUnProcessedVrns().size());
    assertEquals(0, response.getMilitaryVehicleVrns().size());
  }
}