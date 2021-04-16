package uk.gov.caz.vcc.service.bulkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;
import uk.gov.caz.vcc.service.bulkchecker.RetrofitBulkCheckerTestIT.RetrofitTestCase;

@TestPropertySource(properties = "services.mod.use-bulk-search-endpoint=true")
public class ModBulkCheckerTestIT extends BulkCheckerTestIT {

  @BeforeAll
  public void setUp() {
    TEST_DATA.put("CAS300", "Car");
    TEST_DATA.put("CAS318", "Motorcycle");
    TEST_VRNS = new ArrayList<String>(TEST_DATA.keySet());
    mockModForVrns();
  }
  
  @Test
  public void modBulkCheck() throws InterruptedException {
    // act
    List<ComplianceResultsDto> results = callBulkComplianceCheck(TEST_VRNS);

    // assert
    assertEquals(TEST_VRNS.size(), results.size());
    for (ComplianceResultsDto result : results) {
      String vrn = result.getRegistrationNumber();
      assertTrue(TEST_VRNS.contains(vrn));
      assertTrue(result.getIsExempt());
      assertEquals(TEST_DATA.get(vrn), result.getVehicleType());
      for (ComplianceOutcomeDto outcome : result.getComplianceOutcomes()) {
        assertEquals(0.0f, outcome.getCharge());
      }
    }
  }
  
  @Test
  public void modBulkCheckCsv() throws InterruptedException {
    // act
    List<CsvOutput> results = getCsvOfBulkCheck();
    
    // assert
    assertEquals(TEST_VRNS.size(), results.size());
    for (CsvOutput result : results) {
      assertTrue(TEST_VRNS.contains(result.getVrn()));
      assertEquals(TEST_DATA.get(result.getVrn()), result.getVehicleType());
      List<Float> charges = getRawChargesForCsvOutput(result);
      for (int i = 0; i < charges.size(); i++) {
        // this is where a note has been converted to null (as it's not a float)
        if (charges.get(i) == null) {
          continue;
        }
        assertEquals(0, charges.get(i));
      }
    }
    assertSorted(results);
  }

}
