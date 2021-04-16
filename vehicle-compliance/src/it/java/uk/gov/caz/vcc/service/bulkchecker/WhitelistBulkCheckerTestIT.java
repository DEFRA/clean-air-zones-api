package uk.gov.caz.vcc.service.bulkchecker;

import static org.junit.Assert.assertNull;
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

public class WhitelistBulkCheckerTestIT extends BulkCheckerTestIT {

  private Map<String, WhitelistTestCase> TEST_DATA = new HashMap<String, WhitelistTestCase>();

  class WhitelistTestCase {
    String vehicleType;
    boolean taxiPhv;
    boolean exempt;
    boolean compliant;
    Float expectedCharge;
  }

  @BeforeAll
  public void setUp() {
    insertTestData();
    TEST_VRNS = new ArrayList<String>(TEST_DATA.keySet());
  }
  
  @AfterAll
  public void tearDown() {
	  testUtility.deleteGeneralWhiteListVehicles(TEST_VRNS);
  }

  private void insertTestData() {
    TEST_DATA.put("CAS300", createTestCaseAndPersist("CAS300", "Car", false, false, true, 0.0f));
    TEST_DATA.put("CAS307", createTestCaseAndPersist("CAS307", "Van", true, true, false, 0.0f));
    TEST_DATA.put("CAS310", 
        createTestCaseAndPersist("CAS310", "Heavy Goods Vehicle", false, true, false, 0.0f));
    TEST_DATA.put("CAS334", createTestCaseAndPersist("CAS334", "Taxi", true, false, true, 0.0f));
    TEST_DATA.put("NONUK123", createTestCaseAndPersist("NONUK123", "-", false, true, false, 0.0f));
    whenVehiclesAreInTaxiDbBulkForBulkWhitelistTest();
  }

  @Test
  public void whitelistBulkCheck() throws InterruptedException {
    // act
    List<ComplianceResultsDto> results = callBulkComplianceCheck(TEST_VRNS);

    // assert
    assertEquals(TEST_VRNS.size(), results.size());
    for (ComplianceResultsDto result : results) {
      assertTrue(TEST_DATA.containsKey(result.getRegistrationNumber()));
      WhitelistTestCase testCase = TEST_DATA.get(result.getRegistrationNumber());
      if (result.getRegistrationNumber().equals("NONUK123")) {
        assertNull(result.getVehicleType());
      } else {
        assertEquals(testCase.vehicleType, result.getVehicleType());
      }
      assertEquals(testCase.exempt, result.getIsExempt());
      for (ComplianceOutcomeDto outcome : result.getComplianceOutcomes()) {
        if (outcome.getCleanAirZoneId().equals(UUID.fromString(BATH_CAZ))) {
          assertEquals(testCase.expectedCharge, outcome.getCharge());
        } else {
          assertEquals(0, outcome.getCharge());
        }
      }
    }
  }
  
  @Test
  public void csvWhitelistBulkCheck() throws InterruptedException {
    // act
    List<CsvOutput> results = getCsvOfBulkCheck();
    
    // assert
    for (CsvOutput result : results) {
      assertTrue(TEST_DATA.containsKey(result.getVrn()));
      WhitelistTestCase testCase = TEST_DATA.get(result.getVrn());
      assertEquals(testCase.vehicleType, result.getVehicleType());
      if (testCase.exempt) {
        assertNoteIsExempt(result);
      }
      List<Float> charges = getRawChargesForCsvOutput(result);
      for (int i = 0; i < charges.size(); i++) {
        // this is where a note has been converted to null (as it's not a float)
        if (charges.get(i) == null) {
          continue;
        }
        
        if (i == 0) {
          // check against expected charge (which is for bath)
          assertEquals(testCase.expectedCharge, charges.get(i));
        } else {
          // otherwise for Birmingham it should always be non-chargeable
          assertEquals(0, charges.get(i));
        }
      }
    }
    assertSorted(results);
    
  }

  private WhitelistTestCase createTestCaseAndPersist(String vrn, String vehicleType,
      boolean taxiPhv, boolean exempt, boolean compliant, Float expectedCharge) {
    testUtility.createGeneralWhiteListVehicle(vrn, exempt, compliant);
    WhitelistTestCase testCase = new WhitelistTestCase();
    testCase.vehicleType = vehicleType;
    testCase.taxiPhv = taxiPhv;
    testCase.exempt = exempt;
    testCase.compliant = compliant;
    testCase.expectedCharge = expectedCharge;
    return testCase;
  }
}
