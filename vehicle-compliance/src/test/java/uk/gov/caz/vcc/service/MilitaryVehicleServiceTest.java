package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.repository.ModDataProvider;
import uk.gov.caz.vcc.service.MilitaryVehicleService.MilitaryVehicleQueryResponse;

@ExtendWith(MockitoExtension.class)
public class MilitaryVehicleServiceTest {
  @Mock
  private ModDataProvider militaryVehicleRepository;

  private MilitaryVehicleService service;

  @Nested
  class WithSequentialFilterHandler {

    @Test
    public void shouldResponseToModVehicleQuery() {
      mockDataProvider();
      service = new MilitaryVehicleService(militaryVehicleRepository, false);
      List<String> vrns = Arrays.asList("ModRego", "NonModRego", "WillCauseException");
      MilitaryVehicleQueryResponse response =
          service.extractMilitaryVehiclesOutOf(vrns, null, 60);
      assertEquals(1, response.getMilitaryVehicleVrns().size());
      assertEquals(1, response.getUnProcessedVrns().size());
    }

    @Test
    public void shouldHandleInterruptedExceptionGracefully() {
      service = new MilitaryVehicleService(militaryVehicleRepository, false);
      List<String> vrns = Arrays.asList("REGO001", "REGO002", "REGO003");
      Thread.currentThread().interrupt();
      MilitaryVehicleQueryResponse response =
          service.extractMilitaryVehiclesOutOf(vrns, null, 60);
      assertEquals(0, response.getMilitaryVehicleVrns().size());
      assertEquals(3, response.getUnProcessedVrns().size());
    }

    private void mockDataProvider() {
      when(militaryVehicleRepository.existsByVrnIgnoreCase(anyString()))
          .thenReturn(true)
          .thenReturn(false)
          .thenThrow(RuntimeException.class);
    }
  }

  @Nested
  class WithBulkFilterHandler {

    @Test
    public void shouldCallBulkEndpoint() {
      // given
      String militaryVrn = "NO03KN";
      Set<String> vrns = ImmutableSet.of(militaryVrn, "ND84SX", "OI64EFO");
      mockBulkCheckResult(militaryVrn, vrns);
      service = new MilitaryVehicleService(militaryVehicleRepository, true);

      // when
      Set<String> result = service.filterMilitaryVrnsFromList(vrns);

      // then
      assertThat(result).containsOnly(militaryVrn);
    }

    private void mockBulkCheckResult(String militaryVrn, Set<String> vrns) {
      when(militaryVehicleRepository.existByVrns(vrns)).thenReturn(ImmutableMap.of(
          militaryVrn, true,
          "ND84SX", false,
          "OI64EFO", false
      ));
    }

  }
}