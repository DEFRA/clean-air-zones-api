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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;
import uk.gov.caz.vcc.service.bulkchecker.TaxiBulkCheckerTestIT.TaxiTestCase;
import uk.gov.caz.vcc.service.bulkchecker.WhitelistBulkCheckerTestIT.WhitelistTestCase;

/**
 * Class to test bulk check for vehicles which are not registered with any of the data portals, including vehicle recognised by DVLA.
 * @author Informed
 *
 */
public class UnregisteredBulkCheckerTestIT extends BulkCheckerTestIT {
	
  private Map<String, UnregisteredTestCase> TEST_DATA = new HashMap<String, UnregisteredTestCase>();
  
  private static final UUID BATH_CLEAN_AIR_ZONE_ID = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
	
  class UnregisteredTestCase {
    String vehicleType;
    boolean exempt;
    Float bathExpectedCharge;
    Float bhamExpectedCharge;
  }

  
  @BeforeAll
  public void setUp() {
	// check charges for each vehicle type
	TEST_DATA.put("CAS305", createTestCaseAndPersist("CAS305", false, "Bus", 14.0f, 5.5f));
	TEST_DATA.put("POT100", createTestCaseAndPersist("POT100", false, "Bus", 0.0f, 0.0f));
	TEST_DATA.put("CAS340", createTestCaseAndPersist("CAS340", false, "Minibus", 25.0f, 25.0f));
    TEST_DATA.put("CAS301", createTestCaseAndPersist("CAS301", false, "Minibus", 0.0f, 0.0f));
    TEST_DATA.put("IS19AAA", createTestCaseAndPersist("IS19AAA", false, "Car", 0.0f, 0.0f));
    TEST_DATA.put("POT125", createTestCaseAndPersist("POT125", false, "Car", 15.5f, 10.5f));
    TEST_DATA.put("CAS502", createTestCaseAndPersist("CAS502", false, "Heavy Goods Vehicle", 5.69f, 5.69f));
    TEST_DATA.put("IS08QHC", createTestCaseAndPersist("IS08QHC", false, "Heavy Goods Vehicle", 0.0f, 0.0f));
    TEST_DATA.put("POT116", createTestCaseAndPersist("POT116", false, "Van", 0.0f, 0.0f));
    TEST_DATA.put("POT137", createTestCaseAndPersist("POT137", false, "Van", 100.0f, 100.0f));
    TEST_DATA.put("POT136", createTestCaseAndPersist("POT136", false, "Motorcycle", 0.0f, 0.0f));
    TEST_DATA.put("IS00DLA", createTestCaseAndPersist("IS00DLA", false, "Motorcycle", 80.01f, 80.01f));
    TEST_DATA.put("IS19DTA", createTestCaseAndPersist("IS19DTA", true, "Agricultural Vehicle", 0.0f, 0.0f));
    TEST_DATA.put("IS19DTB", createTestCaseAndPersist("IS19DTA", false, "Agricultural Vehicle", 0.0f, 0.0f));
    // check result for an unrecognised vehicles
    TEST_DATA.put("UNKNOWN", createTestCaseAndPersist("UNKNOWN", false, "-", 0.0f, 0.0f));
    // check results for tax class and fuel type exemptions
    TEST_DATA.put("IS05SCA", createTestCaseAndPersist("IS05SCA", true, "Car", 0.0f, 0.0f));
    TEST_DATA.put("IS05EHA", createTestCaseAndPersist("IS05EHA", true, "Heavy Goods Vehicle", 0.0f, 0.0f));
    TEST_DATA.put("IS05ONA", createTestCaseAndPersist("IS05ONA", true, "-", 0.0f, 0.0f));
    TEST_DATA.put("IS05GMA", createTestCaseAndPersist("IS05GMA", true, "Minibus", 0.0f, 0.0f));
    TEST_DATA.put("CAS329", createTestCaseAndPersist("CAS329", true, "Motorcycle", 0.0f, 0.0f));
    TEST_DATA.put("CAS330", createTestCaseAndPersist("CAS330", true, "-", 0.0f, 0.0f));
    TEST_DATA.put("CAS331", createTestCaseAndPersist("CAS331", true, "-", 0.0f, 0.0f));
    TEST_DATA.put("CAS332", createTestCaseAndPersist("CAS332", true, "-", 0.0f, 0.0f));
    // test for disabled tax class
    TEST_DATA.put("CAS339", createTestCaseAndPersist("CAS339", true, "-", 0.0f, 0.0f));
    TEST_VRNS = new ArrayList<String>(TEST_DATA.keySet());
  }

  @Disabled
  @Test
  public void unregisteredBulkCheck() throws InterruptedException {
    // act
    List<ComplianceResultsDto> results = callBulkComplianceCheck(TEST_VRNS);
    // assert
    for (ComplianceResultsDto result : results) {
      UnregisteredTestCase testCase = TEST_DATA.get(result.getRegistrationNumber());
      if (testCase.vehicleType.equals("-")) {
    	assertNull(result.getVehicleType());
      } else {
    	assertEquals(testCase.vehicleType, result.getVehicleType());
      }
      assertEquals(testCase.exempt, result.getIsExempt());
      for (ComplianceOutcomeDto outcome : result.getComplianceOutcomes()) {
    	if (outcome.getCleanAirZoneId().equals(BATH_CLEAN_AIR_ZONE_ID)) {
          // check against bath expected charge
    	  assertEquals(testCase.bathExpectedCharge, outcome.getCharge());
    	} else {
          // otherwise for Birmingham 
          assertEquals(testCase.bhamExpectedCharge, outcome.getCharge());
    	}
      }
    }
  }

  @Disabled
  @Test
  public void unregisteredBulkCheckCsv() throws InterruptedException {
    // act
    List<CsvOutput> results = getCsvOfBulkCheck();
    
    // assert
    for (CsvOutput result : results) {
      UnregisteredTestCase testCase = TEST_DATA.get(result.getVrn());
      assertTrue(TEST_DATA.containsKey(result.getVrn()));
      assertEquals(testCase.vehicleType, result.getVehicleType());
      List<Float> charges = getRawChargesForCsvOutput(result);
      for (int i = 0; i < charges.size(); i++) {
        // this is where a note has been converted to null (as it's not a float)
        if (charges.get(i) == null) {
          continue;
        }
          
        if (i == 0) {
          // check against bath expected charge
          assertEquals(testCase.bathExpectedCharge, charges.get(i));
        } else {
          // otherwise for Birmingham
          assertEquals(testCase.bhamExpectedCharge, charges.get(i));
        }
      }
    }
    assertSorted(results);
  }
  
  private UnregisteredTestCase createTestCaseAndPersist(String vrn, boolean exempt, String vehicleType,
	      Float bathExpectedCharge, Float bhamExpectedCharge) {
	mockTaxiResponse(vrn, false, false);
	UnregisteredTestCase testCase = new UnregisteredTestCase();
	testCase.vehicleType = vehicleType;
	testCase.exempt = exempt;
	testCase.bathExpectedCharge = bathExpectedCharge;
	testCase.bhamExpectedCharge = bhamExpectedCharge;
	return testCase;
  }

}
