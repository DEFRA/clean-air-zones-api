package uk.gov.caz.vcc.service.bulkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;

public class RetrofitBulkCheckerTestIT extends BulkCheckerTestIT {

  private Map<String, RetrofitTestCase> TEST_DATA = new HashMap<String, RetrofitTestCase>();

  class RetrofitTestCase {

    String vehicleType;
    boolean taxiPhv;
    Float expectedCharge;
  }

  @BeforeAll
  public void setUp() {
    TEST_DATA.put("CAS300", createTestCaseAndPersist("CAS300", "Car", false, 0.0f));
    TEST_DATA.put("CAS306", createTestCaseAndPersist("CAS306", "Minibus", false, 0.0f));
    TEST_DATA.put("IS19AAA", createTestCaseAndPersist("IS19AAA", "Taxi", true, 0.0f));
    whenVehiclesAreInTaxiDbBulkForBulkRetrofitTest();
    TEST_VRNS = new ArrayList<String>(TEST_DATA.keySet());
    testUtility.createMultipleRetrofitVehicles(TEST_VRNS);
  }

  @AfterAll
  public void clearDown() {
    testUtility.deleteMultipleRetrofitVehicles(TEST_VRNS);
  }

  @Test
  public void retrofitBulkCheck() throws InterruptedException {
    // act
    List<ComplianceResultsDto> results = callBulkComplianceCheck(TEST_VRNS);

    // assert
    for (ComplianceResultsDto result : results) {
      RetrofitTestCase testCase = TEST_DATA.get(result.getRegistrationNumber());
      assertEquals(testCase.vehicleType, result.getVehicleType());
      assertTrue(result.getIsRetrofitted());
      assertTrue(!result.getIsExempt());
      for (ComplianceOutcomeDto outcome : result.getComplianceOutcomes()) {
        assertEquals(0, outcome.getCharge());
      }
    }
  }

  @Test
  public void retrofitBulkCheckCsv() throws InterruptedException {
    // act
    List<CsvOutput> results = getCsvOfBulkCheck();

    // assert
    for (CsvOutput result : results) {
      RetrofitTestCase testCase = TEST_DATA.get(result.getVrn());
      assertTrue(TEST_DATA.containsKey(result.getVrn()));
      assertEquals(testCase.vehicleType, result.getVehicleType());
      List<Float> charges = getRawChargesForCsvOutput(result);
      for (int i = 0; i < charges.size(); i++) {
        // this is where a note has been converted to null (as it's not a float)
        if (charges.get(i) == null) {
          continue;
        } else {
          assertEquals(0, charges.get(i));
        }
      }
    }
    assertSorted(results);
  }

  private RetrofitTestCase createTestCaseAndPersist(String vrn, String vehicleType,
      boolean taxiPhv, Float expectedCharge) {
    testUtility.createRetrofitVehicle(vrn);
    RetrofitTestCase testCase = new RetrofitTestCase();
    testCase.vehicleType = vehicleType;
    testCase.taxiPhv = taxiPhv;
    testCase.expectedCharge = expectedCharge;
    return testCase;
  }

}
